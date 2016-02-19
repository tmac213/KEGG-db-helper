package keggdbhelper.models;

import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;

import java.util.List;

/**
 * Created by Emilio on 2/17/16.
 */
public class Compound {
    private SimpleStringProperty name;
    private SimpleListProperty<String> ids;

    public Compound(String name) {
        this.name = new SimpleStringProperty(name);
        this.ids = null;
    }

    public String getName() {
        return this.name.get();
    }

    public List<String> getids() {
        return this.ids.get();
    }

    public void setIds(List<String> ids) {
        this.ids = new SimpleListProperty<>(FXCollections.observableArrayList(ids));
    }
}
