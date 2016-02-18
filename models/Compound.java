package keggdbhelper.models;

import javafx.beans.property.SimpleStringProperty;

/**
 * Created by Emilio on 2/17/16.
 */
public class Compound {
    private final SimpleStringProperty name;

    public Compound(String name) {
        this.name = new SimpleStringProperty(name);
    }

    public String getName() {
        return this.name.get();
    }
}
