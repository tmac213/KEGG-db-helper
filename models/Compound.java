package keggdbhelper.models;

/**
 * Created by Emilio on 2/17/16.
 */
public class Compound {
    String name;

    public Compound(String name) {
        this.name = name;
    }

    public String name() {
        return this.name;
    }
}
