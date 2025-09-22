package com.example.ainovel.service.world;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class WorkspaceWorldModulesView extends AbstractMap<String, WorkspaceWorldModule> implements Iterable<WorkspaceWorldModule> {

    private final Map<String, WorkspaceWorldModule> delegate;

    public WorkspaceWorldModulesView(Map<String, WorkspaceWorldModule> delegate) {
        this.delegate = Collections.unmodifiableMap(new LinkedHashMap<>(delegate));
    }

    @Override
    public Set<Entry<String, WorkspaceWorldModule>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public WorkspaceWorldModule get(Object key) {
        return delegate.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public Iterator<WorkspaceWorldModule> iterator() {
        return delegate.values().iterator();
    }

    public Collection<WorkspaceWorldModule> valuesList() {
        return delegate.values();
    }
}
