package wikimap.view

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.text.FontWeight
import javafx.scene.text.TextAlignment
import wikimap.models.MindMapNode
import tornadofx.*
import wikimap.utils.DragResizeMod
import wikimap.views.ChangeEvent

class NodeView(val main: MainView, val model: MindMapNode, val isSuggestion: Boolean = false): StackPane() {

    val onChange = ChangeEvent()

    private val isSelectedProperty = SimpleBooleanProperty(false)
    var isSelected by isSelectedProperty

    val rect: Rectangle =
        Rectangle(0.0, 0.0).apply {
            arcWidth = main.gridSpacing.toDouble()
            arcHeight = main.gridSpacing.toDouble()
            fill = Color(Math.random(), Math.random(), Math.random(), 0.7)
        }

    val label = Label(model.key).apply {
        style {
            textFill = Color.WHITE
            fontWeight = FontWeight.BOLD
            alignment = Pos.CENTER
            textAlignment = TextAlignment.CENTER
            wrapText = true
        }
        textProperty().bind(model.keyProperty)
    }

    val textArea = TextArea(model.key).apply {
        style {
            textFill = Color.WHITE
            fontWeight = FontWeight.BOLD
            wrapText = true
            backgroundColor = multi(Color.TRANSPARENT)
        }
        textProperty().bindBidirectional(model.keyProperty)
        paddingTop = 10
        paddingBottom = 10
        isVisible = false
    }

    private val selectedBorder = Border(BorderStroke(
        Color.BLUE, BorderStrokeStyle.SOLID,
        CornerRadii.EMPTY, BorderWidths(1.0)
    ))

    private val resizeListener = object : DragResizeMod.OnDragResizeEventListener {
        override fun onResize(n: Node?, x: Double, y: Double, h: Double, w: Double) {
            val (nx, ny) = main.gridView.toGridCoords(x, y)
            val cx = Math.round(nx).toInt()
            val cy = Math.round(ny).toInt()

            if (nx != model.x.toDouble()) {
                model.width = (model.x + model.width) - cx
            } else {
                model.width = Math.round(w / main.gridSpacing).toInt()
            }

            if (ny != model.y.toDouble()) {
                model.height = (model.y + model.height) - cy
            } else {
                model.height = Math.round(h / main.gridSpacing).toInt()
            }

            model.x = cx
            model.y = cy

            model.width = maxOf(3, model.width)
            model.height = maxOf(3, model.height)

            refresh()
        }

        override fun onDrag(n: Node?, x: Double, y: Double, h: Double, w: Double) {
            val (nx, ny) = main.gridView.toGridCoords(x, y)
            model.x = Math.round(nx).toInt()
            model.y = Math.round(ny).toInt()
            refresh()
        }
    }

    constructor(main: MainView, model: MindMapNode, suggestionParent: NodeView)
        : this(main, model, true) {

        val totalOffsetX = model.x - suggestionParent.model.x
        val totalOffsetY = model.y - suggestionParent.model.y

        val offsetX = totalOffsetX +
            if (totalOffsetX < 0) model.width else -suggestionParent.model.width

        val offsetY = totalOffsetY +
            if (totalOffsetY < 0) model.height else -suggestionParent.model.height

        rect.fill = Color(0.0, 0.0, 0.0, 0.2)

        onHover {
            if (it) {
                rect.fill = Color(0.2, 0.2, 0.2, 0.2)
            } else {
                rect.fill = Color(0.0, 0.0, 0.0, 0.2)
            }
        }

        onDoubleClick { main.includeSuggestion(suggestionParent, this) }

        suggestionParent.onChange += {
            model.x = suggestionParent.model.x + offsetX
            model.y = suggestionParent.model.y + offsetY

            if (offsetX < 0) model.x -= model.width
            else             model.x += suggestionParent.model.width

            if (offsetY < 0) model.y -= model.height
            else             model.y += suggestionParent.model.height

            refresh()
        }
    }

    init {
        this += rect
        this += label
        this += textArea


        borderProperty().bind(
            isSelectedProperty.objectBinding {
                if (it ?: false) selectedBorder else Border.EMPTY
            }
        )

        textArea.focusedProperty().onChange {
            if (!it) hideTextArea()
        }

        DragResizeMod.makeResizable(this, resizeListener)

        if (!isSuggestion) {
            label.onDoubleClick {
                showTextArea()
            }

            onMouseClicked = EventHandler {
                main.selectNodes(this)
            }
        }

        main.onChange += this::refresh
        refresh()
    }

    fun getCenterX(): Double = main.gridView.fromGridCoords(model.x + model.width/2.0, 0.0).first
    fun getCenterY(): Double = main.gridView.fromGridCoords(0.0, model.y + model.height/2.0).second

    private fun refresh() {
        val (x, y) = main.gridView.fromGridCoords(model.x.toDouble(), model.y.toDouble())
        rect.width = model.width.toDouble() * main.gridSpacing
        rect.height = model.height.toDouble() * main.gridSpacing
        relocate(x, y)
        setPrefSize(rect.width, rect.height)

        onChange.fireChange()
        toFront()
    }

    private fun getAllChildren(node: Node): List<Node> {
        val children = node.getChildList()?.toList() ?: listOf()
        return children + children.flatMap { getAllChildren(it) }.toList()
    }

    private fun showTextArea() {
        label.isVisible = false
        textArea.isVisible = true

        val scrollPane = textArea.childrenUnmodifiable[0] as ScrollPane
        scrollPane.vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        for (child in getAllChildren(textArea)) {
            child.style { backgroundColor = multi(Color(0.0,0.0,0.0,0.05)) }
            child.isCache = false // Prevent blurry text
        }

        textArea.requestFocus()
    }

    private fun hideTextArea() {
        label.isVisible = true
        textArea.isVisible = false
    }
}