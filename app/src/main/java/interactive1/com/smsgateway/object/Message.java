package interactive1.com.smsgateway.object;

/**
 * Created by dtomic on 07/07/15.
 */
public class Message {

    private String phoneNumber;
    private int id;
    private String text;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
