package Project.client.views;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.font.TextAttribute;
import java.awt.Dimension;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import Project.client.ClientUtils;
import Project.client.ICardControls;

public class UserListPanel extends JPanel {
    JPanel userListArea;
    private static Logger logger = Logger.getLogger(UserListPanel.class.getName());
    private Map<Long, JEditorPane> userMap;
    private long lastMessageSenderId;

    public UserListPanel(ICardControls controls) {
        super(new BorderLayout(10, 10));
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setAlignmentY(Component.BOTTOM_ALIGNMENT);

        // wraps a viewport to provide scroll capabilities
        JScrollPane scroll = new JScrollPane(content);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        // scroll.setBorder(BorderFactory.createEmptyBorder());
        // no need to add content specifically because scroll wraps it

        userListArea = content;
        userMap = new HashMap<>();
        lastMessageSenderId = -1;

        wrapper.add(scroll);
        this.add(wrapper, BorderLayout.CENTER);

        userListArea.addContainerListener(new ContainerListener() {

            @Override
            public void componentAdded(ContainerEvent e) {
                if (userListArea.isVisible()) {
                    userListArea.revalidate();
                    userListArea.repaint();
                }
            }

            @Override
            public void componentRemoved(ContainerEvent e) {
                if (userListArea.isVisible()) {
                    userListArea.revalidate();
                    userListArea.repaint();
                }
            }

        });
    }

    protected void addUserListItem(long clientId, String clientName) {
        logger.log(Level.INFO, "Adding user to list: " + clientName);
        JPanel content = userListArea;
        logger.log(Level.INFO, "Userlist: " + content.getSize());
        JEditorPane textContainer = new JEditorPane("text/plain", clientName);
        textContainer.setName(clientId + "");
        // sizes the panel to attempt to take up the width of the container
        // and expand in height based on word wrapping
        textContainer.setLayout(null);
        textContainer.setPreferredSize(
                new Dimension(content.getWidth(), ClientUtils.calcHeightForText(this, clientName, content.getWidth())));
        textContainer.setMaximumSize(textContainer.getPreferredSize());
        textContainer.setEditable(false);
        // remove background and border (comment these out to see what it looks like
        // otherwise)
        ClientUtils.clearBackground(textContainer);
        // add to container
        content.add(textContainer);
        userMap.put(clientId, textContainer); // test
    }

    protected void removeUserListItem(long clientId) {
        logger.log(Level.INFO, "removing user list item for id " + clientId);
        Component[] cs = userListArea.getComponents();
        for (Component c : cs) {
            if (c.getName().equals(clientId + "")) {
                userListArea.remove(c);
                break;
            }
        }
    }

    protected void clearUserList() {
        Component[] cs = userListArea.getComponents();
        for (Component c : cs) {
            userListArea.remove(c);
        }
    }

    protected void updateUserMuteStatus(long clientId, boolean isMuted) {  //  UCID: mth39, Date: 04/30/24, Milestone 4
        JEditorPane textContainer = userMap.get(clientId);
        if (textContainer != null) {
            Font font = textContainer.getFont();
            Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
            attributes.put(TextAttribute.FOREGROUND, isMuted ? Color.GRAY : Color.BLACK);
            textContainer.setFont(font.deriveFont(attributes));
        }
    }

    protected void highlightLastMessageSender(long clientId) {  //  UCID: mth39, Date: 04/30/24, Milestone 4
        if (lastMessageSenderId != -1) {
            JEditorPane lastMessageSender = userMap.get(lastMessageSenderId);
            if (lastMessageSender != null) {
                Font font = lastMessageSender.getFont();
                Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
                attributes.put(TextAttribute.FOREGROUND, Color.BLACK);
                lastMessageSender.setFont(font.deriveFont(attributes));
            }
        }
        JEditorPane currentMessageSender = userMap.get(clientId);
        if (currentMessageSender != null) {
            Font font = currentMessageSender.getFont();
            Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
            attributes.put(TextAttribute.FOREGROUND, Color.BLUE);
            currentMessageSender.setFont(font.deriveFont(attributes));
            lastMessageSenderId = clientId;
        }
    }
}

