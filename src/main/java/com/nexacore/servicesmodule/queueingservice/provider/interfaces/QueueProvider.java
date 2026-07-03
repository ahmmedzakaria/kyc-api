package com.nexacore.servicesmodule.queueingservice.provider.interfaces;

import com.nexacore.servicesmodule.queueingservice.dto.QueueMessageRequest;
import com.nexacore.servicesmodule.queueingservice.dto.QueueMessageResponse;

public interface QueueProvider {

    String providerName();

    QueueMessageResponse publish(QueueMessageRequest request);
}
