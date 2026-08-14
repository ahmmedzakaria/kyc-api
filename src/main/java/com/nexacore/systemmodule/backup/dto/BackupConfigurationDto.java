package com.nexacore.systemmodule.backup.dto;
public record BackupConfigurationDto(boolean enabled,String schedule,String zone,int retentionDays,int minimumSuccessful,
                                     long timeoutSeconds,String storageType,boolean encryptionConfigured,String encryptionKeyId,
                                     boolean deliveryEnabled,int recipientCount,boolean attachmentEnabled,long attachmentMaxBytes){}
