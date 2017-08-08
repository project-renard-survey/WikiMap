package wikimap.view

import javafx.geometry.Orientation
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TitledPane
import javafx.scene.control.cell.TextFieldListCell
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import tornadofx.*
import wikimap.app.BasicSuggestionProvider
import wikimap.utils.NumericTextField
import wikimap.utils.SuggestionsCache

/**
 * Created by Basim on 04/08/2017.
 */
class SelectionView(val main: MainView) : Pane() {

    val contentPane = VBox()

    val propertiesGrid = GridPane()

    val xLabel = NumericTextField()
    val yLabel = NumericTextField()
    val widthLabel = NumericTextField()
    val heightLabel = NumericTextField()

    val suggestionsList = ListView<String>()
    private var oldKey = ""
    val suggestionProvider = SuggestionsCache(BasicSuggestionProvider())

    val root = TitledPane("Edit Node", contentPane)

    init {
        contentPane.label("Node Properties")

        propertiesGrid.hgap = 5.0
        propertiesGrid.vgap = 5.0

        propertiesGrid.add(Label("X"), 0, 0)
        propertiesGrid.add(Label("Y"), 0, 1)
        propertiesGrid.add(Label("Width"), 0, 2)
        propertiesGrid.add(Label("Height"), 0, 3)

        propertiesGrid.add(xLabel, 1, 0)
        propertiesGrid.add(yLabel, 1, 1)
        propertiesGrid.add(widthLabel, 1, 2)
        propertiesGrid.add(heightLabel, 1, 3)

        xLabel.isDisable = true
        yLabel.isDisable = true
        widthLabel.isDisable = true
        heightLabel.isDisable = true

        xLabel.valueProperty.onChange {
            if (!xLabel.isDisable) {
                main.selectedNodes.forEach { it.model.x = xLabel.value.toInt() }
                main.refresh()
            }
        }

        yLabel.valueProperty.onChange {
            if (!yLabel.isDisable) {
                main.selectedNodes.forEach { it.model.y = yLabel.value.toInt() }
                main.refresh()
            }
        }

        widthLabel.valueProperty.onChange {
            if (!widthLabel.isDisable) {
                main.selectedNodes.forEach { it.model.width = widthLabel.value.toInt() }
                main.refresh()
            }
        }

        heightLabel.valueProperty.onChange {
            if (!heightLabel.isDisable) {
                main.selectedNodes.forEach { it.model.height = heightLabel.value.toInt() }
                main.refresh()
            }
        }
        contentPane += propertiesGrid

        contentPane.label("Node Suggestions")
        suggestionsList.orientation = Orientation.VERTICAL
        suggestionsList.cellFactory = TextFieldListCell.forListView()
        contentPane += suggestionsList

        this.layoutBoundsProperty().onChange {
            root.prefWidth = layoutBounds.width
            root.prefHeight = layoutBounds.height
        }

        root.isCollapsible = false
        this += root

        main.onChange += this::refresh
        main.selectedNodes.onChange { refresh() }

        refresh()
    }

    private fun <T, V> allSame(xs: List<T>, pred: (T) -> V): Boolean {
        if (xs.isEmpty()) return true
        val x = pred(xs.first())
        return xs.all { pred(it) == x }
    }

    private fun refresh() {
        refreshProperties()
        refreshSuggestions()
    }

    private fun refreshProperties() {

        if (main.selectedNodes.isEmpty()) {
            xLabel.isDisable = true
            yLabel.isDisable = true
            widthLabel.isDisable = true
            heightLabel.isDisable = true

        } else {

            xLabel.isDisable = false
            yLabel.isDisable = false
            widthLabel.isDisable = false
            heightLabel.isDisable = false

            val first = main.selectedNodes.first()

            if (allSame(main.selectedNodes, { it.model.x })) {
                xLabel.value = first.model.x
            } else {
                xLabel.clear()
            }
            if (allSame(main.selectedNodes, { it.model.y })) {
                yLabel.value = first.model.y
            } else {
                yLabel.clear()
            }
            if (allSame(main.selectedNodes, { it.model.width })) {
                widthLabel.value = first.model.width
            } else {
                widthLabel.clear()
            }
            if (allSame(main.selectedNodes, { it.model.height })) {
                heightLabel.value = first.model.height
            } else {
                heightLabel.clear()
            }

            for (node in main.selectedNodes) node.onChange += this::refreshProperties
        }

    }

    private fun refreshSuggestions() {
        if (main.selectedNodes.isEmpty()) {
            if (oldKey != "") {
                suggestionsList.items.clear()
                oldKey = ""
            }

            return
        }

        val key = main.selectedNodes.first().keyText.get()

        if (key != oldKey) {
            suggestionsList.items.clear()
            suggestionsList.items.addAll(suggestionProvider.getSuggestions(key))
            oldKey = key
        }
    }
}