package com.redhat.lightblue.hook.audit;

import com.redhat.lightblue.metadata.HookConfiguration;

/**
 *
 * @author nmalik
 */
public class AuditHookConfiguration implements HookConfiguration {
    private final String entityName;
    private final String version;

    public AuditHookConfiguration(String entityName, String version) {
        this.entityName = entityName;
        this.version = version;
    }

    /**
     * @return the entityName
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }
}
