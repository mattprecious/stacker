package com.mattprecious.stacker.command.downstack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import com.jakewharton.mosaic.LocalTerminal
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.layout.widthIn
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.ui.Arrangement.Absolute.spacedBy
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Layout
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.collections.ancestors
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.command.perform
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.InteractivePrompt
import com.mattprecious.stacker.rendering.PromptState
import com.mattprecious.stacker.rendering.branch
import com.mattprecious.stacker.rendering.code
import com.mattprecious.stacker.rendering.promptItem
import com.mattprecious.stacker.rendering.toAnnotatedString
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

fun StackerDeps.downstackEdit(): StackerCommand {
	return DownstackEdit(
		configManager = configManager,
		locker = locker,
		stackManager = stackManager,
		vc = vc,
	)
}

internal class DownstackEdit(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand() {
	override suspend fun StackerCommandScope.work() {
		val currentBranchName = vc.currentBranchName
		val currentBranch = stackManager.getBranch(currentBranchName)
		if (currentBranch == null) {
			printStaticError(
				buildAnnotatedString {
					append("Cannot edit downstack since ")
					branch { append(currentBranchName) }
					append(" is not tracked. Please track with ")
					code { append("st branch track") }
					append(".")
				},
			)
			abort()
		}

		val trunk = configManager.trunk
		val trailingTrunk = configManager.trailingTrunk

		val downstack = (sequenceOf(currentBranch) + currentBranch.ancestors).map { it.name }.toList()
		val firstTrunkIndex = downstack.indexOfFirst { it == trunk || it == trailingTrunk }

		val downstackTrunk = downstack[firstTrunkIndex]
		val downstackWithoutTrunk = downstack.take(firstTrunkIndex)

		if (downstackWithoutTrunk.size < 2) {
			if (currentBranch.name == trunk || currentBranch.name == trailingTrunk) {
				printStaticError("Not on a stack.")
			} else {
				printStaticError("Stack only has one branch.")
			}

			abort()
		}

		val result = render { onResult ->
			Edit(
				stack = downstackWithoutTrunk.toPersistentList(),
				trunk = downstackTrunk,
				onResult = onResult,
			)
		}

		val newStack = result.filter { it.state == EditState.Item.State.Keep }
		val deleted = result.filter { it.state != EditState.Item.State.Keep }

		newStack.windowed(size = 2, step = 1, partialWindows = true).forEach {
			// We temporarily make branches inaccessible while changing parents one-by-one, so pass
			// strings rather than Branch references.
			stackManager.updateParent(
				branch = it.first().name,
				parent = it.getOrNull(1)?.name ?: downstackTrunk,
			)
		}

		deleted.forEach {
			val branch = stackManager.getBranch(it.name)!!
			when (it.state) {
				EditState.Item.State.Untrack -> stackManager.untrackBranch(branch.value)
				EditState.Item.State.Remove -> {
					stackManager.updateParent(
						branch = stackManager.getBranch(branch.name)!!.value,
						parent = stackManager.getBranch(downstackTrunk)!!.value,
					)
				}

				EditState.Item.State.Delete -> {
					if (branch.name == currentBranchName) {
						vc.checkout(branch.parent!!.name)
					}

					stackManager.untrackBranch(branch.value)
					vc.delete(branch.name)
				}

				else -> throw IllegalStateException()
			}
		}

		val operation = Locker.Operation.Restack(
			startingBranch = vc.currentBranchName,
			newStack.reversed().map { it.name },
		)

		locker.beginOperation(operation) {
			operation.perform(this@work, this@beginOperation, stackManager, vc)
		}
	}
}

@Composable
private fun Edit(
	stack: ImmutableList<String>,
	trunk: String,
	onResult: (List<EditState.Item>) -> Unit,
) {
	val state = remember(stack) { EditState(stack) }
	var deletingItem by remember { mutableStateOf<EditState.Item?>(null) }

	Column(
		verticalArrangement = spacedBy(1),
	) {
		ListDetail(
			modifier = Modifier.widthIn(max = LocalTerminal.current.size.width),
			list = {
				MainList(
					state = state,
					trunk = trunk,
					showingDeletePrompt = deletingItem != null,
					onDelete = { deletingItem = it },
					onFinish = { onResult(state.items.toList()) },
				)
			},
			detail = deletingItem?.let {
				{
					DeletePrompt(
						item = it,
						trunk = trunk,
						onFinish = { deletingItem = null },
					)
				}
			},
			detailHeader = deletingItem?.let {
				{
					Text(
						buildAnnotatedString {
							append("What would you like to do with ")
							branch { append(it.name) }
							append("?")
						},
					)
				}
			},
		)

		Text(
			value = """
				|Move branches up/down by holding Shift.
				|Remove branch by pressing Backspace/Delete.
			""".trimMargin(),
			textStyle = TextStyle.Dim,
		)
	}
}

private class EditState(
	items: ImmutableList<String>,
) {
	val items = items.map(::Item).toMutableStateList()
	var selected by mutableIntStateOf(0)
		private set

	fun moveDown() {
		selected = (selected + 1).coerceAtMost(items.size - 1)
	}

	fun moveUp() {
		selected = (selected - 1).coerceAtLeast(0)
	}

	fun moveItemDown(): Boolean {
		if (selected == items.size - 1) return false
		items.add(selected + 1, items.removeAt(selected))
		moveDown()
		return true
	}

	fun moveItemUp(): Boolean {
		if (selected == 0) return false
		items.add(selected - 1, items.removeAt(selected))
		moveUp()
		return true
	}

	class Item(
		val name: String,
	) {
		var state by mutableStateOf(State.Keep)

		enum class State {
			Keep,
			Remove,
			Untrack,
			Delete,
		}
	}
}

@Composable
private fun ListDetail(
	list: @Composable () -> Unit,
	detail: (@Composable () -> Unit)?,
	detailHeader: (@Composable () -> Unit)?,
	modifier: Modifier = Modifier,
) {
	Layout(
		modifier = modifier,
		content = {
			list()
			detail?.invoke()
			detailHeader?.invoke()
		},
	) { measurables, constraints ->
		val listMeasurable = measurables[0]
		val detailMeasurable = measurables.getOrNull(1)
		val detailHeaderMeasurable = measurables.getOrNull(2)

		val listWidth = listMeasurable.maxIntrinsicWidth(constraints.maxHeight)
		val detailWidth = detailMeasurable?.maxIntrinsicWidth(constraints.maxHeight) ?: 0
		val gap = if (detailWidth != 0) 2 else 0

		val sideBySide = constraints.maxWidth >= listWidth + detailWidth + gap
		val listPlaceable = if (detailMeasurable == null || sideBySide) {
			listMeasurable.measure(constraints.copy(minWidth = listWidth, maxWidth = listWidth))
		} else {
			null
		}

		val detailPlaceable = detailMeasurable?.measure(
			constraints.copy(minWidth = detailWidth, maxWidth = detailWidth),
		)

		val detailHeaderPlaceable = if (detailMeasurable == null || sideBySide) {
			null
		} else {
			detailHeaderMeasurable?.measure(constraints)
		}

		val listHeight = listPlaceable?.height ?: 0
		val measuredListWidth = listPlaceable?.width ?: 0

		val detailsHeight = detailPlaceable?.height ?: 0
		val measuredDetailsWidth = detailPlaceable?.width ?: 0

		val detailsHeaderHeight = detailHeaderPlaceable?.height ?: 0

		val width = measuredListWidth + measuredDetailsWidth +
			if (measuredListWidth > 0 && measuredDetailsWidth > 0) 2 else 0
		val height = maxOf(listHeight, detailsHeight) + detailsHeaderHeight

		layout(width, height) {
			detailHeaderPlaceable?.place(0, 0)
			listPlaceable?.place(0, detailsHeaderHeight)

			val detailsX = if (listPlaceable == null) 0 else listPlaceable.width + gap
			detailPlaceable?.place(detailsX, detailsHeaderHeight)
		}
	}
}

@Composable
private fun MainList(
	state: EditState,
	trunk: String,
	showingDeletePrompt: Boolean,
	onDelete: (EditState.Item) -> Unit,
	onFinish: () -> Unit,
) {
	Column(
		modifier = if (!showingDeletePrompt) {
			Modifier.onKeyEvent {
				when {
					it.key == "Enter" -> onFinish()
					it.shift && it.key == "ArrowUp" -> return@onKeyEvent state.moveItemUp()
					it.shift && it.key == "ArrowDown" -> return@onKeyEvent state.moveItemDown()
					it.key == "ArrowUp" -> state.moveUp()
					it.key == "ArrowDown" -> state.moveDown()
					it.key == "Delete" || it.key == "Backspace" -> {
						val item = state.items[state.selected]
						if (state.items[state.selected].state == EditState.Item.State.Keep) {
							onDelete(item)
						} else {
							item.state = EditState.Item.State.Keep
						}
					}

					else -> return@onKeyEvent false
				}

				return@onKeyEvent true
			}
		} else {
			Modifier
		},
	) {
		state.items.forEachIndexed { index, item ->
			val text = buildAnnotatedString {
				promptItem(index == state.selected) {
					val strikethroughIndex = if (item.state != EditState.Item.State.Keep) {
						pushStyle(SpanStyle(textStyle = TextStyle.Strikethrough))
					} else {
						null
					}

					append(item.name)

					strikethroughIndex?.let(::pop)
				}

				when (item.state) {
					EditState.Item.State.Keep -> {}
					EditState.Item.State.Remove -> append(" (remove)")
					EditState.Item.State.Untrack -> append(" (untrack)")
					EditState.Item.State.Delete -> append(" (delete)")
				}
			}

			Text(text)
		}

		Text(
			value = remember { buildAnnotatedString { promptItem(false) { append(trunk) } } },
			textStyle = TextStyle.Dim,
		)
	}
}

private enum class RemovalOption {
	Remove,
	Untrack,
	Delete,
	Cancel,
}

@Composable
private fun DeletePrompt(
	item: EditState.Item,
	trunk: String,
	onFinish: () -> Unit,
) {
	InteractivePrompt(
		modifier = Modifier.onKeyEvent {
			// TODO: I don't really want to support ArrowLeft if we're not in SBS mode. Not sure how to do
			//  that. A scope?
			if (it.key == "Escape" || it.key == "ArrowLeft") {
				onFinish()
				true
			} else {
				false
			}
		},
		message = null,
		state = remember {
			PromptState(
				options = RemovalOption.entries.toPersistentList(),
				default = null,
				displayTransform = {
					when (it) {
						RemovalOption.Remove -> buildAnnotatedString {
							append("Remove from stack, set parent to ")
							branch { append(trunk) }
						}

						RemovalOption.Untrack -> "Untrack it".toAnnotatedString()
						RemovalOption.Delete -> "Delete it".toAnnotatedString()
						RemovalOption.Cancel -> "Cancel".toAnnotatedString()
					}
				},
				valueTransform = { throw IllegalStateException() },
			)
		},
		filteringEnabled = false,
		staticPrintResult = false,
		onSelected = {
			when (it) {
				RemovalOption.Remove -> item.state = EditState.Item.State.Remove
				RemovalOption.Untrack -> item.state = EditState.Item.State.Untrack
				RemovalOption.Delete -> item.state = EditState.Item.State.Delete
				RemovalOption.Cancel -> {}
			}

			onFinish()
		},
	)
}
