package com.bt.pi.app.instancemanager.testing;

import com.bt.pi.core.mail.MailSender;

public class StubMailSender extends MailSender {
    private String lastMessage = null;

    @Override
    public void send(String to, String subject, String text) {
        lastMessage = text;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void reset() {
        lastMessage = null;
    }
}
