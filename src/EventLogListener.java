public interface EventLogListener {

    public void eventAdded(Event evt);
    public void eventRemoved(Event evt);
    public void reloadEvents();

}

