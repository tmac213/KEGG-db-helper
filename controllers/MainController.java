package keggdbhelper.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Callback;
import keggdbhelper.helpers.OutputGenerator;
import keggdbhelper.models.Compound;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class MainController implements Initializable {
    public TableView<Compound> table;

    /**
     * Called to initialize a controller after its root element has been
     * completely processed.
     *
     * @param location  The location used to resolve relative paths for the root object, or
     *                  <tt>null</tt> if the location is not known.
     * @param resources The resources used to localize the root object, or <tt>null</tt> if
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void chooseFile(ActionEvent actionEvent) {

        Window mainWindow = table.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a .xls or .xlsx File");
        File chosenFile = fileChooser.showOpenDialog(mainWindow);

        HashSet<Compound> compoundsToSearch = extractCompoundsFromFile(chosenFile);
        buildTable(compoundsToSearch);

        new Thread(() -> {
            OutputGenerator.generateOutput(compoundsToSearch);
        }).start();
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

                                try {
                                    FXMLLoader loader = new FXMLLoader(getClass().getResource("../views/CompoundView.fxml"));
                                    Stage stage = new Stage(StageStyle.DECORATED);
                                    stage.setTitle("Compound");
                                    stage.setScene(new Scene(loader.load()));

                                    CompoundController cc = loader.<CompoundController>getController();
                                    cc.initData(((TableCell)event.getSource()).getText());

                                    stage.show();

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

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

    private HashSet<Compound> extractCompoundsFromFile(File xlsFile) {
        if (xlsFile == null) {
            System.out.println("File was null");
            return null;
        }

        Iterator<Row> rowIterator = getRowIteratorForFile(xlsFile);

        if (rowIterator == null) {
            System.err.println("MainController: getRowIteratorForFile returned null.");
            return null;
        }

        HashSet<Compound> ret = new HashSet<>();

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

    private Iterator<Row> getRowIteratorForFile(File xlsFile) {
        try {
            FileInputStream inputStream = new FileInputStream(xlsFile);
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
