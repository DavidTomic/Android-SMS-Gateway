package interactive1.com.smsgateway.object;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dtomic on 07/07/15.
 */
public class MessageSource {

    private List<Message> messageList;
    private List<Integer> successMessagesIdList;
    private List<Integer> successMessagesIdListTemp;
    private List<Integer> unSuccessMessagesIdList;
    private List<Integer> unSuccessMessagesIdListTemp;

    private static MessageSource instance;
    private boolean lock;

    private MessageSource() {

        messageList = new ArrayList<Message>();
        successMessagesIdList = new ArrayList<Integer>();
        successMessagesIdListTemp = new ArrayList<Integer>();
        unSuccessMessagesIdList = new ArrayList<Integer>();
        unSuccessMessagesIdListTemp = new ArrayList<Integer>();
    }

    public static synchronized MessageSource getInstance() {
        initInstance();
        return instance;
    }

    public static synchronized void initInstance() {
        if (instance == null) {
            instance = new MessageSource();
        }
    }


    public boolean isLock() {
        return lock;
    }

    public void setLock(boolean lock) {
        this.lock = lock;
    }


    public List<Message> getMessageList() {

        return messageList;
    }

    public List<Integer> getSuccessMessagesIdList() {
        return successMessagesIdList;
    }


    public List<Integer> getSuccessMessagesIdListTemp() {
        return successMessagesIdListTemp;
    }

    public List<Integer> getUnSuccessMessagesIdList() {
        return unSuccessMessagesIdList;
    }

    public List<Integer> getUnSuccessMessagesIdListTemp() {
        return unSuccessMessagesIdListTemp;
    }

    public void empty(){
        instance = null;
    }

}
