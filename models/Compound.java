package keggdbhelper.models;

import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import keggdbhelper.helpers.OutputGenerator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Emilio on 2/17/16.
 */
public class Compound {

    private SimpleStringProperty name;
    private SimpleStringProperty resultName;
    private SimpleListProperty<Entry> entries;

    public Compound(String name) {
        this.name = new SimpleStringProperty(name);
        this.resultName = new SimpleStringProperty();
        this.entries = new SimpleListProperty<>();
    }

    public String getName() {
        return this.name.get();
    }

    public String getResultName() { return this.resultName.get(); }

    public void setResultName(String resultName) { this.resultName.set(resultName); }

    public List<Entry> getEntries() {
        return this.entries.get();
    }

    public void setEntries(List<Entry> entries) {
        this.entries.set(FXCollections.observableList(entries));
    }

    public String outputString() {
        if (this.entries.isEmpty()) {
            return String.format("%s\n<i>Searching compound name returned no results.</i><br><br>\n", this.headerString());
        }
        return String.format("%s\n%s<br>\n", this.headerString(), this.entryListString());
    }

    private String headerString() {
        return String.format(OutputGenerator.HEADER_TEMPLATE, this.name.get());
    }

    private String entryListString() {
        return String.format(OutputGenerator.LIST_TEMPLATE, String.join("\n<br>", this.entryStrings()));
    }

    private List<String> entryStrings() {
        return this.entries.get().stream()
                .map(Entry::entryString)
                .collect(Collectors.<String>toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Compound compound = (Compound) o;

        return name.get().equals(compound.name.get());
    }

    @Override
    public int hashCode() {
        return name.get().hashCode();
    }
}
