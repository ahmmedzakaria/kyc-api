package com.nexacore.servicesmodule.queueingservice.service.interfaces;

import com.nexacore.servicesmodule.queueingservice.dto.QueueMessageRequest;
import com.nexacore.servicesmodule.queueingservice.dto.QueueMessageResponse;

public interface QueueingService {

    QueueMessageResponse publish(QueueMessageRequest request);
}
