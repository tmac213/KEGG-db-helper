package keggdbhelper.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Window;
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
    public Label search;
    public TableView<Compound> table;
    private String pathToXMLFile;


    public void performSearch(ActionEvent actionEvent) {
        search.setText(search.getText() + "!");
    }

    public void chooseFile(ActionEvent actionEvent) {

        Window mainWindow = search.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select an XML File");
        File chosenFile = fileChooser.showOpenDialog(mainWindow);

        ArrayList<Compound> compoundsToSearch = extractCompoundsFromFile(chosenFile);
        buildTable(compoundsToSearch);

        for (Compound c : compoundsToSearch) {
            System.out.println(c.getName());
        }
    }

    private void buildTable(Collection<Compound> compounds) {
        table.setEditable(true);
        table.getColumns().clear();

        ObservableList<Compound> tableData = FXCollections.observableArrayList(compounds);

        TableColumn compoundNameColumn = new TableColumn("Compound Name");
        compoundNameColumn.setCellValueFactory(new PropertyValueFactory<Compound, String>("name"));

        table.setItems(tableData);
        table.getColumns().add(compoundNameColumn);
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
