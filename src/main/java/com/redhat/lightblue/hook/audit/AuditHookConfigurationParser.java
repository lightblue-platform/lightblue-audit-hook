package com.redhat.lightblue.hook.audit;

import com.redhat.lightblue.hooks.CRUDHook;
import com.redhat.lightblue.metadata.HookConfiguration;
import com.redhat.lightblue.metadata.parser.HookConfigurationParser;
import com.redhat.lightblue.metadata.parser.MetadataParser;

/**
 * Parser for AuditHookConfiguration.
 *
 * @author nmalik
 */
public class AuditHookConfigurationParser<T> implements HookConfigurationParser<T> {

    @Override
    public String getName() {
        return AuditHook.HOOK_NAME;
    }

    @Override
    public CRUDHook getCRUDHook() {
        return new AuditHook();
    }

    @Override
    public HookConfiguration parse(String name, MetadataParser<T> p, T node) {
        return new AuditHookConfiguration();
    }

    @Override
    public void convert(MetadataParser<T> p, T emptyNode, HookConfiguration object) {
        //nothing to convert
    }
}
