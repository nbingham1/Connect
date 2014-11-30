package mobilesystems.connect.utils;

/**
 * Created by Oliver on 11/11/2014.
 */
public class ValuePair {
    private String name;
    private String value;

    public ValuePair(String value, String name)
    {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
