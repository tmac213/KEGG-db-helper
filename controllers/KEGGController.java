package keggdbhelper.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Callback;
import keggdbhelper.models.Compound;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class KEGGController {
    public TableView<Compound> table;


    public void chooseFile(ActionEvent actionEvent) {

        Window mainWindow = table.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select an XML File");
        File chosenFile = fileChooser.showOpenDialog(mainWindow);

        ArrayList<Compound> compoundsToSearch = extractCompoundsFromFile(chosenFile);
        buildTable(compoundsToSearch);
    }

    private void buildTable(Collection<Compound> compounds) {
        table.setEditable(true);
        table.getColumns().clear();
        ObservableList<Compound> tableData = FXCollections.observableArrayList(compounds);

        Callback<TableColumn, TableCell> cellFactory =
                new Callback<TableColumn, TableCell>() {
                    @Override
                    public TableCell call(TableColumn param) {
                        TableCell cell = new TableCell<Compound, String>() {
                            @Override
                            public void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                setText(empty ? null : getString());
                                setGraphic(null);
                            }

                            private String getString() {
                                return getItem() == null ? "" : getItem().toString();
                            }
                        };

                        cell.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                            @Override
                            public void handle(MouseEvent event) {
                                System.out.println(event.getSource().toString() + " clicked.");
                                // get results for compound
                            }
                        });
                        return cell;
                    }
                };

        TableColumn compoundNameColumn = new TableColumn("Compound Name");
        compoundNameColumn.setPrefWidth(600);
        compoundNameColumn.setCellValueFactory(new PropertyValueFactory<Compound, String>("name"));
        compoundNameColumn.setCellFactory(cellFactory);

        table.setItems(tableData);
        table.getColumns().add(compoundNameColumn);

        table.setEditable(false);
    }

    private ArrayList<Compound> extractCompoundsFromFile(File xmlFile) {
        if (xmlFile == null) {
            System.out.println("File was null");
            return null;
        }

        Iterator<Row> rowIterator = getRowIteratorForFile(xmlFile);

        if (rowIterator == null) {
            System.err.println("KEGGController: getRowIteratorForFile returned null.");
            return null;
        }

        ArrayList<Compound> ret = new ArrayList<Compound>();

        while (rowIterator.hasNext()) {
            Row currentRow = rowIterator.next();
            Iterator<Cell> cellIterator = currentRow.cellIterator();

            while (cellIterator.hasNext()) {
                Cell currentCell = cellIterator.next();

                switch (currentCell.getCellType()) {
                    case Cell.CELL_TYPE_STRING:
                        if (currentCell.getStringCellValue().contains("TMS")) {
                            ret.add(extractCompoundFromCell(currentCell));
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return ret;
    }

    private Compound extractCompoundFromCell(Cell cell) {
        String[] split = cell.getStringCellValue().split("-");
        StringBuilder ret = new StringBuilder();
        ret.append(split[0]);
        for (int i = 1; i < split.length - 1; ++i) {
            if (split[i].contains("meto")) {
                break;
            }
            ret.append('-');
            ret.append(split[i]);
        }
        return new Compound(ret.toString());
    }

    private Iterator<Row> getRowIteratorForFile(File xmlFile) {
        try {
            FileInputStream inputStream = new FileInputStream(xmlFile);
            Workbook workbook = new XSSFWorkbook(inputStream);
            return workbook.getSheetAt(0).rowIterator();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
