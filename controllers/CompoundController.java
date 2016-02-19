package keggdbhelper.controllers;

import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import keggdbhelper.helpers.OutputGenerator;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by Emilio on 2/19/16.
 */
public class CompoundController implements Initializable {
    public TextArea textArea;

    public void initData(String data) {
        displayCompoundInfo(data);
    }

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

    private void displayCompoundInfo(String compoundName) {
        List<String> resultIDs = searchByCompound(compoundName);
        String resultInfo = searchByIDs(resultIDs);

        StringBuilder sb = new StringBuilder();

        for (String s : resultIDs) {
            sb.append(s);
            sb.append('\n');
        }

        textArea.setText(String.format(sb.toString()));
    }

    private List<String> searchByCompound(String name) {
        return OutputGenerator.getIDs(name);
    }

    private String searchByIDs(List<String> ids) {
        return "";
    }
}
