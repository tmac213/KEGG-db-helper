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
    private static String HEADER_TEMPLATE = "<h3>%s</h3>";
    private static String LINK_LIST_TEMPLATE = "<ul>\n%s\n</ul>";

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
        if (this.ids.isEmpty()) {
            return String.format("%s\n<i>No results found.</i><br><br>\n", this.headerString());
        }
        return String.format("%s\n%s<br>\n", this.headerString(), this.linkListString());
    }

    private String headerString() {
        return String.format(HEADER_TEMPLATE, this.name.get());
    }

    private String linkListString() {
        return String.format(LINK_LIST_TEMPLATE ,String.join("\n<br>", this.idLinks()));
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
