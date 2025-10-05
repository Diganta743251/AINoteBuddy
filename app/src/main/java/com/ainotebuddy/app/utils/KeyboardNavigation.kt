package com.ainotebuddy.app.utils

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.BorderStroke

/**
 * Keyboard navigation utilities for accessibility and power users
 */

class KeyboardNavigationState {
    private val _focusRequesters = mutableMapOf<Int, FocusRequester>()
    val focusRequesters: Map<Int, FocusRequester> get() = _focusRequesters
    
    private var currentFocusIndex by mutableStateOf(0)
    private val focusableItems = mutableListOf<Int>()
    
    fun registerFocusable(id: Int): FocusRequester {
        val requester = FocusRequester()
        _focusRequesters[id] = requester
        if (id !in focusableItems) {
            focusableItems.add(id)
            focusableItems.sort()
        }
        return requester
    }
    
    fun unregisterFocusable(id: Int) {
        _focusRequesters.remove(id)
        focusableItems.remove(id)
    }
    
    fun focusNext() {
        if (focusableItems.isEmpty()) return
        
        val nextIndex = (currentFocusIndex + 1) % focusableItems.size
        val nextId = focusableItems[nextIndex]
        _focusRequesters[nextId]?.requestFocus()
        currentFocusIndex = nextIndex
    }
    
    fun focusPrevious() {
        if (focusableItems.isEmpty()) return
        
        val prevIndex = if (currentFocusIndex <= 0) {
            focusableItems.size - 1
        } else {
            currentFocusIndex - 1
        }
        val prevId = focusableItems[prevIndex]
        _focusRequesters[prevId]?.requestFocus()
        currentFocusIndex = prevIndex
    }
    
    fun focusFirst() {
        if (focusableItems.isNotEmpty()) {
            val firstId = focusableItems.first()
            _focusRequesters[firstId]?.requestFocus()
            currentFocusIndex = 0
        }
    }
    
    fun focusLast() {
        if (focusableItems.isNotEmpty()) {
            val lastId = focusableItems.last()
            _focusRequesters[lastId]?.requestFocus()
            currentFocusIndex = focusableItems.size - 1
        }
    }
}

@Composable
fun rememberKeyboardNavigationState(): KeyboardNavigationState {
    return remember { KeyboardNavigationState() }
}

@Composable
fun KeyboardNavigationHandler(
    navigationState: KeyboardNavigationState,
    content: @Composable () -> Unit
) {
    val focusManager = LocalFocusManager.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { keyEvent ->
                when {
                    keyEvent.key == Key.Tab && keyEvent.type == KeyEventType.KeyDown -> {
                        if (keyEvent.isShiftPressed) {
                            navigationState.focusPrevious()
                        } else {
                            navigationState.focusNext()
                        }
                        true
                    }
                    keyEvent.key == Key.Home && keyEvent.type == KeyEventType.KeyDown -> {
                        navigationState.focusFirst()
                        true
                    }
                    keyEvent.key == Key.MoveEnd && keyEvent.type == KeyEventType.KeyDown -> {
                        navigationState.focusLast()
                        true
                    }
                    keyEvent.key == Key.Escape && keyEvent.type == KeyEventType.KeyDown -> {
                        focusManager.clearFocus()
                        true
                    }
                    else -> false
                }
            }
            .focusable()
    ) {
        content()
    }
}

@Composable
fun NavigableFocusable(
    id: Int,
    navigationState: KeyboardNavigationState,
    modifier: Modifier = Modifier,
    onEnterPressed: (() -> Unit)? = null,
    onSpacePressed: (() -> Unit)? = null,
    content: @Composable (FocusRequester) -> Unit
) {
    val focusRequester = remember(id) {
        navigationState.registerFocusable(id)
    }
    
    DisposableEffect(id) {
        onDispose {
            navigationState.unregisterFocusable(id)
        }
    }
    
    Box(
        modifier = modifier
            .onKeyEvent { keyEvent ->
                when {
                    keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown -> {
                        onEnterPressed?.invoke()
                        true
                    }
                    keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyDown -> {
                        onSpacePressed?.invoke()
                        true
                    }
                    else -> false
                }
            }
    ) {
        content(focusRequester)
    }
}

@Composable
fun NavigableButton(
    id: Int,
    navigationState: KeyboardNavigationState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    NavigableFocusable(
        id = id,
        navigationState = navigationState,
        onEnterPressed = if (enabled) onClick else null,
        onSpacePressed = if (enabled) onClick else null
    ) { focusRequester ->
        Button(
            onClick = onClick,
            modifier = modifier.focusRequester(focusRequester),
            enabled = enabled,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content
        )
    }
}

@Composable
fun NavigableTextField(
    id: Int,
    value: String,
    onValueChange: (String) -> Unit,
    navigationState: KeyboardNavigationState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: androidx.compose.ui.graphics.Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors()
) {
    NavigableFocusable(
        id = id,
        navigationState = navigationState
    ) { focusRequester ->
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.focusRequester(focusRequester),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            interactionSource = interactionSource,
            shape = shape,
            colors = colors
        )
    }
}

@Composable
fun NavigableCard(
    id: Int,
    navigationState: KeyboardNavigationState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit
) {
    NavigableFocusable(
        id = id,
        navigationState = navigationState,
        onEnterPressed = if (enabled) onClick else null,
        onSpacePressed = if (enabled) onClick else null
    ) { focusRequester ->
        Card(
            onClick = onClick,
            modifier = modifier.focusRequester(focusRequester),
            enabled = enabled,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            interactionSource = interactionSource,
            content = content
        )
    }
}

@Composable
fun NavigableCheckbox(
    id: Int,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    navigationState: KeyboardNavigationState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    NavigableFocusable(
        id = id,
        navigationState = navigationState,
        onEnterPressed = if (enabled && onCheckedChange != null) {
            { onCheckedChange(!checked) }
        } else null,
        onSpacePressed = if (enabled && onCheckedChange != null) {
            { onCheckedChange(!checked) }
        } else null
    ) { focusRequester ->
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier.focusRequester(focusRequester),
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource
        )
    }
}

@Composable
fun NavigableSwitch(
    id: Int,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    navigationState: KeyboardNavigationState,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    NavigableFocusable(
        id = id,
        navigationState = navigationState,
        onEnterPressed = if (enabled && onCheckedChange != null) {
            { onCheckedChange(!checked) }
        } else null,
        onSpacePressed = if (enabled && onCheckedChange != null) {
            { onCheckedChange(!checked) }
        } else null
    ) { focusRequester ->
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier.focusRequester(focusRequester),
            thumbContent = thumbContent,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource
        )
    }
}

/**
 * Keyboard shortcuts handler for global app shortcuts
 */
@Composable
fun AppKeyboardShortcuts(
    onNewNote: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    onUndo: (() -> Unit)? = null,
    onRedo: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.isCtrlPressed) {
                    when (keyEvent.key) {
                        Key.N -> {
                            onNewNote?.invoke()
                            true
                        }
                        Key.F -> {
                            onSearch?.invoke()
                            true
                        }
                        Key.Comma -> {
                            onSettings?.invoke()
                            true
                        }
                        Key.S -> {
                            onSave?.invoke()
                            true
                        }
                        Key.Z -> {
                            if (keyEvent.isShiftPressed) {
                                onRedo?.invoke()
                            } else {
                                onUndo?.invoke()
                            }
                            true
                        }
                        Key.Y -> {
                            onRedo?.invoke()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        content()
    }
}

/**
 * Focus management utilities
 */
object FocusUtils {
    @Composable
    fun AutoFocus(focusRequester: FocusRequester) {
        LaunchedEffect(focusRequester) {
            focusRequester.requestFocus()
        }
    }
    
    @Composable
    fun ClearFocusOnBackPress() {
        val focusManager = LocalFocusManager.current
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Back && keyEvent.type == KeyEventType.KeyDown) {
                        focusManager.clearFocus()
                        true
                    } else {
                        false
                    }
                }
        )
    }
}