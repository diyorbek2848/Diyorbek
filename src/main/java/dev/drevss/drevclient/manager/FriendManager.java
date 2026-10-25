package dev.drevss.drevclient.manager;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class FriendManager {
    private final Set<String> friends = new TreeSet<>();

    public boolean isFriend(String name) {
        return name != null && friends.contains(name.toLowerCase(Locale.ROOT));
    }

    public boolean add(String name) { return friends.add(name.toLowerCase(Locale.ROOT)); }
    public boolean remove(String name) { return friends.remove(name.toLowerCase(Locale.ROOT)); }
    public void clear() { friends.clear(); }
    public Collection<String> all() { return Collections.unmodifiableSet(friends); }
}
