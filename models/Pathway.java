package keggdbhelper.models;

import javafx.beans.property.SimpleStringProperty;

/**
 * Created by Emilio on 2/26/16.
 */
public class Pathway {
    private SimpleStringProperty name;
    private SimpleStringProperty id;

    public Pathway(String name, String id) {
        this.name = new SimpleStringProperty(name);
        this.id = new SimpleStringProperty(id);
    }

    public String getName() { return name.get(); }

    public String getId() { return id.get(); }

    static String pathwayString(Pathway pathway, Entry entry) {
        return String.format("<li><a href=\"http://www.genome.jp/kegg-bin/show_pathway?%s+%s\">%s</a>: %s</li>", pathway.getId(), entry.getId(), pathway.getId(), pathway.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Pathway pathway = (Pathway) obj;

        return this.id.get().equals(pathway.id.get());
    }

    @Override
    public int hashCode() {
        return id.get().hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s: %s", id.get(), name.get());
    }
}
