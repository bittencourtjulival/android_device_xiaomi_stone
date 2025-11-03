#=============================================================================
# Copyright (c) 2021 Qualcomm Technologies, Inc.
# All Rights Reserved.
# Confidential and Proprietary - Qualcomm Technologies, Inc.
#
# Copyright (c) 2012-2013, 2016-2020, The Linux Foundation. All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are
# met:
#     * Redistributions of source code must retain the above copyright
#       notice, this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above
#       copyright notice, this list of conditions and the following
#       disclaimer in the documentation and/or other materials provided
#       with the distribution.
#     * Neither the name of The Linux Foundation nor the names of its
#       contributors may be used to endorse or promote products derived
#       from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
# WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
# ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
# BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
# BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
# WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
# OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
# IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#=============================================================================

function configure_zram_parameters() {
    local zramSizeGB=$(getprop persist.vendor.zram.size)
    local zramComp=$(getprop persist.vendor.zram.comp_algorithm)
    local swappiness=$(getprop persist.vendor.vm.swappiness)

    # Set swappiness
    echo ${swappiness:-60} > /proc/sys/vm/swappiness

    case "$zramSizeGB" in
        0)
            zRamSizeMB=0
            echo "ZRAM disabled by user choice."
            return
            ;;
        2)
            zRamSizeMB=2048
            ;;
        4)
            zRamSizeMB=4096
            ;;
        8)
            zRamSizeMB=8192
            ;;
        *)
            # Default dynamic calculation
            MemTotalStr=$(grep MemTotal /proc/meminfo)
            MemTotal=${MemTotalStr:16:8}
            let RamSizeGB="( $MemTotal / 1048576 ) + 1"
            if [ $RamSizeGB -le 2 ]; then
                let zRamSizeMB="( $RamSizeGB * 1024 ) * 3 / 4"
            else
                let zRamSizeMB="( $RamSizeGB * 1024 ) / 2"
            fi
            [ $zRamSizeMB -gt 4096 ] && zRamSizeMB=4096
            ;;
    esac

    if [ -f /sys/block/zram0/disksize ]; then
        # Set compression algorithm
        if [ -n "$zramComp" ] && grep -q "$zramComp" /sys/block/zram0/comp_algorithm; then
            echo "$zramComp" > /sys/block/zram0/comp_algorithm
        else
            echo "lz4" > /sys/block/zram0/comp_algorithm
        fi

        if [ -f /sys/block/zram0/use_dedup ]; then
            echo 1 > /sys/block/zram0/use_dedup
        fi
        echo "${zRamSizeMB}M" > /sys/block/zram0/disksize

        if [ -e /sys/kernel/slab/zs_handle ]; then
            echo 0 > /sys/kernel/slab/zs_handle/store_user
        fi
        if [ -e /sys/kernel/slab/zspage ]; then
            echo 0 > /sys/kernel/slab/zspage/store_user
        fi

        mkswap /dev/block/zram0
        swapon /dev/block/zram0 -p 32758
    fi
}

function configure_memory_parameters() {
    # Disable wsf for all targets because we are using efk.
    # wsf Range : 1..1000 So set to bare minimum value 1.
    echo 1 > /proc/sys/vm/watermark_scale_factor
    configure_zram_parameters

    # Spawn 1 kswapd threads which can help in fast reclaiming of pages
    echo 1 > /proc/sys/vm/kswapd_threads
}

# configure governor settings for silver cluster
echo "schedutil" > /sys/devices/system/cpu/cpufreq/policy0/scaling_governor

# configure governor settings for gold cluster
echo "schedutil" > /sys/devices/system/cpu/cpufreq/policy6/scaling_governor

echo N > /sys/module/lpm_levels/parameters/sleep_disabled

configure_memory_parameters

# Let kernel know our image version/variant/crm_version
if [ -f /sys/devices/soc0/select_image ]; then
    image_version="10:"
    image_version+=`getprop ro.build.id`
    image_version+=":"
    image_version+=`getprop ro.build.version.incremental`
    image_variant=`getprop ro.product.name`
    image_variant+="-"
    image_variant+=`getprop ro.build.type`
    oem_version=`getprop ro.build.version.codename`
    echo 10 > /sys/devices/soc0/select_image
    echo $image_version > /sys/devices/soc0/image_version
    echo $image_variant > /sys/devices/soc0/image_variant
    echo $oem_version > /sys/devices/soc0/image_crm_version
fi

# Change console log level as per console config property
console_config=`getprop persist.vendor.console.silent.config`
case "$console_config" in
    "1")
        echo "Enable console config to $console_config"
        echo 0 > /proc/sys/kernel/printk
        ;;
    *)
        echo "Enable console config to $console_config"
        ;;
esac

setprop vendor.post_boot.parsed 1

echo 0 > /proc/sys/vm/panic_on_oom
