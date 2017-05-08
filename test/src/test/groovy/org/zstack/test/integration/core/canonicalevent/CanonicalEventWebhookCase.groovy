package org.zstack.test.integration.core.canonicalevent

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CanonicalEvent
import org.zstack.core.cloudbus.EventFacade
import org.zstack.sdk.WebhookInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by xing5 on 2017/5/8.
 */
class CanonicalEventWebhookCase extends SubCase {
    EnvSpec envSpec

    @Override
    void clean() {
        envSpec.delete()
    }

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
        spring {
            include("webhook.xml")
        }
    }

    String WEBHOOK_PATH = "/canonical-event-webhook"

    void testCanonicalEventUseWebhook() {
        String path = "/test/event"

        WebhookInventory hook1 = createWebhook {
            name = "webhook1"
            url = "http://127.0.0.1:8989$WEBHOOK_PATH"
            type = EventFacade.WEBHOOK_TYPE
            opaque = path
        }

        WebhookInventory hook2 = createWebhook {
            name = "webhook2"
            url = "http://127.0.0.1:8989$WEBHOOK_PATH"
            type = EventFacade.WEBHOOK_TYPE
            opaque = path
        }

        List<CanonicalEvent> evts = []
        envSpec.simulator(WEBHOOK_PATH) { HttpEntity<String> e ->
            CanonicalEvent evt = json(e.getBody(), CanonicalEvent.class)
            evts.add(evt)
            return [:]
        }

        String content = "hello world"
        bean(EventFacade.class).fire(path, content)

        retryInSecs {
            return {
                assert evts.size() == 2
                CanonicalEvent evt1 = evts[0]
                CanonicalEvent evt2 = evts[1]
                assert evt1.path == path
                assert evt1.content == content
                assert evt1.managementNodeId == Platform.getManagementServerId()
                assert evt2.path == path
                assert evt2.content == content
                assert evt2.managementNodeId == Platform.getManagementServerId()
            }
        }
    }

    @Override
    void environment() {
        envSpec = env {
            // nothing
        }
    }

    @Override
    void test() {
        envSpec.create {
            testCanonicalEventUseWebhook()
        }
    }
}
