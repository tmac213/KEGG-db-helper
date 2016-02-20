package keggdbhelper.models;

import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;

import java.util.List;
import java.util.stream.Collectors;

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

    public String outputString() {
        return String.format("<h3>%s</h3>\n<br><ul>\n%s\n</ul><br>\n", this.name.get(), String.join("\n<br>", this.idLinks()));
    }

    private List<String> idLinks() {
        return this.ids.get().stream()
                .map(Compound::idLink)
                .collect(Collectors.<String>toList());
    }

    private static String idLink(String id) {
        return String.format("<li><a href=\"http://www.genome.jp/dbget-bin/www_bget?cpd:%s\">%s</a></li>", id, id);
    }
}
