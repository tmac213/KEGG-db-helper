package keggdbhelper.models;

import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Emilio on 2/26/16.
 */
public class Entry {
    private SimpleStringProperty id;
    private SimpleStringProperty name;
    private SimpleListProperty<Pathway> pathways;

    public Entry(String id) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty();
        this.pathways = new SimpleListProperty<>();
    }

    public String getId() {
        return id.get();
    }

    public void setId(String id) {
        this.id.set(id);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public List<Pathway> getPathways() { return this.pathways.get(); }

    public void setPathways(List<Pathway> pathways) { this.pathways.set(FXCollections.observableList(pathways)); }

    static String entryString(Entry entry) {
        List<String> pathwayStrings = entry.pathwayStrings();
        if (pathwayStrings.isEmpty()) {
            pathwayStrings.add("<i>Entry does not link to any pathways.</i>");
        }
        return String.format("<li><a href=\"http://www.genome.jp/dbget-bin/www_bget?cpd:%s\">%s</a>: %s\n<ul>%s\n</ul></li>", entry.getId(), entry.getId(), entry.getName(),  String.join("\n", pathwayStrings));
    }

    private List<String> pathwayStrings() {
        return this.pathways.get().stream()
                .map(pathway -> Pathway.pathwayString(pathway, this))
                .collect(Collectors.<String>toList());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Entry entry = (Entry) obj;

        return this.id.get().equals(entry.id.get());
    }

    @Override
    public int hashCode() {
        return id.get().hashCode();
    }
}
