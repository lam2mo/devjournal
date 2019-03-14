import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.text.*;
import java.util.*;

public class EventLogViewer extends JPanel 
    implements ActionListener, EventLogListener {
    
    private EventLog eventLog;
    private Window parent;
    private Map<Event,EventPanel> eventLookup;
    private Component verticalGlue;

    private JPanel mainList;

    private JPanel buttonPanel;
    private JButton reloadButton;
    private JButton clearSelButton;
    private JButton mergeButton;
    private JButton splitButton;
    private JButton deleteButton;
    private JButton saveButton;
    private JButton exitButton;

    public EventLogViewer(EventLog log) {
        this.eventLog = log;
        this.eventLog.addEventLogListener(this);
        this.eventLookup = new HashMap<Event,EventPanel>();
        this.parent = null;
        this.verticalGlue = null;

        mainList = new JPanel();
        mainList.setBackground(Color.WHITE);
        mainList.setLayout(new BoxLayout(mainList, BoxLayout.PAGE_AXIS));

        JScrollPane scrollPane = new JScrollPane(mainList);
        //scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        //scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        buttonPanel = new JPanel();

        reloadButton = new JButton("Reload All");
        reloadButton.addActionListener(this);
        clearSelButton = new JButton("Clear Selection");
        clearSelButton.addActionListener(this);
        mergeButton = new JButton("Merge");
        mergeButton.addActionListener(this);
        splitButton = new JButton("Split");
        splitButton.addActionListener(this);
        deleteButton = new JButton("Delete");
        deleteButton.addActionListener(this);
        saveButton = new JButton("Save Log");
        saveButton.addActionListener(this);
        exitButton = new JButton("Exit");
        exitButton.addActionListener(this);
        buttonPanel.add(reloadButton);
        buttonPanel.add(clearSelButton);
        buttonPanel.add(mergeButton);
        buttonPanel.add(splitButton);
        buttonPanel.add(deleteButton);
        //buttonPanel.add(saveButton);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        reload();
    }

    public void setParent(Window parent) {
        this.parent = parent;
        if (parent != null) {
            buttonPanel.add(exitButton);
        }
    }

    public void setExitButtonLabel(String text) {
        exitButton.setText(text);
    }

    public void reload() {
        mainList.removeAll();
        eventLookup.clear();
        for (Event evt : eventLog.getAllEvents()) {
            EventPanel evp = new EventPanel(evt, this);
            mainList.add(evp);
            eventLookup.put(evt, evp);
        }
        if (verticalGlue == null) {
            verticalGlue = Box.createVerticalGlue();
        }
        mainList.add(verticalGlue);
        mainList.revalidate();
        mainList.repaint();
    }

    public void clearSelection() {
        Component[] comps = mainList.getComponents();
        int i;
        for (i=0; i<comps.length; i++) {
            if (comps[i] instanceof EventPanel) {
                EventPanel evp = (EventPanel)comps[i];
                evp.setSelected(false);
            }
        }
    }

    public void mergeSelection() {
        java.util.List<Event> events = getSelectedEvents();
        if (events.size() > 0) {
            eventLog.mergeEvents(events);
            eventLog.saveToDisk();
        }
    }

    public void splitSelection() {
        java.util.List<Event> events = getSelectedEvents();
        if (events.size() > 0) {
            for (Event evt : events) {
                eventLog.splitEvent(evt);
            }
            eventLog.saveToDisk();
        }
    }

    public void deleteSelection() {
        java.util.List<Event> events = getSelectedEvents();
        if (events.size() > 0) {
            int rval = JOptionPane.showConfirmDialog(this, 
                    "Are you sure you want to delete the " + events.size() + " selected event(s)?",
                    "Delete Events", JOptionPane.YES_NO_OPTION);
            if (rval == JOptionPane.YES_OPTION) {
                for (Event evt : getSelectedEvents()) {
                    eventLog.removeEvent(evt);
                }
                eventLog.saveToDisk();
            }
        }
    }

    public java.util.List<Event> getSelectedEvents() {
        java.util.List<Event> events = new ArrayList();
        Component[] comps = mainList.getComponents();
        int i;
        for (i=0; i<comps.length; i++) {
            if (comps[i] instanceof EventPanel && ((EventPanel)comps[i]).isSelected()) {
                events.add(((EventPanel)comps[i]).getEvent());
            }
        }
        return events;
    }

    public void eventAdded(Event evt) {
        if (verticalGlue == null) {
            verticalGlue = Box.createVerticalGlue();
        } else {
            mainList.remove(verticalGlue);
        }
        EventPanel evp = new EventPanel(evt, this);
        mainList.add(evp);
        eventLookup.put(evt, evp);
        mainList.add(verticalGlue);
        mainList.revalidate();
        mainList.repaint();
    }

    public void eventRemoved(Event evt) {
        EventPanel evp = eventLookup.get(evt);
        eventLookup.remove(evt);
        mainList.remove(evp);
        mainList.revalidate();
        mainList.repaint();
    }

    public void reloadEvents() {
        reload();
    }
    
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == reloadButton) {
            reload();
        } else if (e.getSource() == clearSelButton) {
            clearSelection();
        } else if (e.getSource() == mergeButton) {
            mergeSelection();
        } else if (e.getSource() == splitButton) {
            splitSelection();
        } else if (e.getSource() == deleteButton) {
            deleteSelection();
        } else if (e.getSource() == saveButton) {
            eventLog.saveToDisk();
        } else if (e.getSource() == exitButton) {
            if (parent != null) {
                parent.dispose();
            }
        }
    }

}

