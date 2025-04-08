#
# Copyright (C) 2023-2024 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
TARGET_SUPPORTS_OMX_SERVICE := false
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit from device.
$(call inherit-product, $(LOCAL_PATH)/device.mk)

# Inherit some common Lineage stuff.
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

# Blur
TARGET_USES_BLUR := true

# Boot animation
TARGET_SCREEN_HEIGHT := 2400
TARGET_SCREEN_WIDTH := 1080

PRODUCT_BRAND := Redmi
PRODUCT_DEVICE := stone
PRODUCT_MANUFACTURER := Xiaomi
PRODUCT_MODEL := Redmi Note 12 5G
PRODUCT_NAME := lineage_stone

# GMS
PRODUCT_GMS_CLIENTID_BASE := android-xiaomi
TARGET_USES_PICO_GAPPS := true

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="sunstone_global-user 14 UKQ1.230917.001 V816.0.10.0.UMQMIXM release-keys" \
    BuildFingerprint=Redmi/sunstone_global/sunstone:14/UKQ1.230917.001/V816.0.10.0.UMQMIXM:user/release-keys \
    DeviceProduct=sunstone
