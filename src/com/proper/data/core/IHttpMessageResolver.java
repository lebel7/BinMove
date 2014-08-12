package com.proper.data.core;

import android.content.Context;
import com.proper.data.Contact;
import com.proper.messagequeue.Message;

import java.util.List;

/**
 * Created by Lebel on 16/05/2014.
 */
public interface IHttpMessageResolver {
    String resolveMessageQuery(Message msg);
    String resolveMessageAction(Message msg);
    List<Contact> resolveContacts(Context context);
}
