package keggdbhelper.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
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
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class MainController implements Initializable {

    private static final Logger log= Logger.getLogger( MainController.class.getName() );
    static {
        try {
            FileHandler fileHandler = new FileHandler(System.getProperty("user.dir") + "/MainController.log");
            log.addHandler(fileHandler);
            fileHandler.setFormatter(new SimpleFormatter());
            log.setLevel(Level.ALL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TableView<Compound> tableView;
    public CheckBox listByCompoundCheckBox;
    public CheckBox listByPathwayCheckBox;
    public Circle progressCircle;
    public TextField organismTextField;

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
        Window mainWindow = tableView.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a .xls or .xlsx File");
        File chosenFile = fileChooser.showOpenDialog(mainWindow);

        if (chosenFile != null) {
            progressCircle.setFill(Paint.valueOf("#FF0000"));  // red
            HashSet<Compound> compoundsToSearch = extractCompoundsFromFile(chosenFile);
            buildTable(compoundsToSearch);

            new Thread(() -> {
                OutputGenerator.Options options = new OutputGenerator.Options(organismTextField.getText(), listByCompoundCheckBox.isSelected(), listByPathwayCheckBox.isSelected());
                OutputGenerator.generateOutput(compoundsToSearch, String.format("%s-%s-output-%s", chosenFile.getName(), new Date().toString().replace(' ', '-'), options.getOrganismCode()), options);
                progressCircle.setFill(Paint.valueOf("#00FF00"));  // green
            }).start();
        }
    }

    private void buildTable(Collection<Compound> compounds) {
        tableView.setEditable(true);
        tableView.getColumns().clear();
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
                                return getItem() == null ? "" : getItem();
                            }
                        };

                        cell.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                            @Override
                            public void handle(MouseEvent event) {
                                log.log(Level.FINE, event.getSource().toString() + " clicked.");

                                try {
                                    FXMLLoader loader = new FXMLLoader(getClass().getResource("../views/CompoundView.fxml"));
                                    Stage stage = new Stage(StageStyle.DECORATED);
                                    stage.setTitle("Compound");
                                    stage.setScene(new Scene(loader.load()));

                                    CompoundController cc = loader.<CompoundController>getController();
                                    cc.initData(((TableCell)event.getSource()).getText());

                                    stage.show();

                                } catch (IOException e) {
                                    log.log(Level.SEVERE, e.getMessage());
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

        tableView.setItems(tableData);
        tableView.getColumns().add(compoundNameColumn);

        tableView.setEditable(false);
    }

    private HashSet<Compound> extractCompoundsFromFile(File xlsFile) {
        if (xlsFile == null) {
            log.log(Level.WARNING, "File was null");
            return null;
        }

        Iterator<Row> rowIterator = getRowIteratorForFile(xlsFile);

        if (rowIterator == null) {
            log.log(Level.WARNING, "MainController: getRowIteratorForFile returned null.");
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
            log.log(Level.SEVERE, e.getMessage());
            return null;
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
            return null;
        }
    }
}
