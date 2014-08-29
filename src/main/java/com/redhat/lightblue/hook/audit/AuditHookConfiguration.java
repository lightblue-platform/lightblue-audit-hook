package com.redhat.lightblue.hook.audit;

import com.redhat.lightblue.metadata.HookConfiguration;

/**
 *
 * @author nmalik
 */
public class AuditHookConfiguration implements HookConfiguration {
    private final String entityName;
    private final String version;
    private final String lightblueCrudURI;

    public AuditHookConfiguration(String entityName, String version, String lightblueCrudURI) {
        this.entityName = entityName;
        this.version = version;
        this.lightblueCrudURI = lightblueCrudURI;
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

    /**
     * @return the lightblueCrudUri
     */
    public String getLightblueCrudURI() {
        return lightblueCrudURI;
    }
}
