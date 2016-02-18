package keggdbhelper.controllers;

import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

public class KEGGController {
    public Label search;
    private String pathToXMLFile;


    public void performSearch(ActionEvent actionEvent) {
        search.setText(search.getText() + "!");
    }

    public void chooseFile(ActionEvent actionEvent) {

        Window mainWindow = search.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select an XML File");
        File chosenFile = fileChooser.showOpenDialog(mainWindow);

        if (chosenFile != null) {
            System.out.println(chosenFile.getName());
            pathToXMLFile = chosenFile.getAbsolutePath();
            System.out.println(pathToXMLFile);
        }
    }
}
