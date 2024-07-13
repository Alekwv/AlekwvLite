package net.runelite.client.plugins.AlekwvFisher;

import net.runelite.client.config.*;

@ConfigGroup("alekwvfisher")
public interface AlekwvFisherConfig extends Config
{
    @ConfigSection(
            name = "Basic settings",
            description = "",
            position = 0
    )
    String basicSection = "Basic settings";

    enum locations
    {
        NONE,
        LUMBRIDGE_SWAMP,
        BARBARIAN_VILLAGE
    }

    @ConfigItem(
            keyName = "location",
            name = "Location",
            description = "",
            position = 1,
            section = basicSection
    )

    default locations location() { return locations.NONE; }

    @ConfigItem(
            keyName = "world",
            name = "World",
            description = "",
            section = basicSection
    )
    @Range(min = 301, max = 589)
    default int getWorld()
    {
        return 308;
    }

    @ConfigSection(
            name = "Antiban settings",
            description = "",
            position = 2
    )
    String antiBanSection = "AntiBan settings";

    @ConfigItem(
            keyName = "antiban_mousemovement",
            name = "Move mouse",
            description = "",
            section = antiBanSection
    )
    @Units(Units.PERCENT)
    @Range(min = 0, max = 100)
    default int antibanvalue_cameramovement()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "antiban_cameramovement",
            name = "Move camera",
            description = "",
            section = antiBanSection
    )
    @Units(Units.PERCENT)
    @Range(min = 0, max = 100)
    default int antibanvalue_mousemovement()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "antiban_mouseclick",
            name = "Click mouse",
            description = "",
            section = antiBanSection
    )
    @Units(Units.PERCENT)
    @Range(min = 0, max = 100)
    default int antibanvalue_clickmouse()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "antiban_mouseoutsidescreen",
            name = "Mouse outside screen",
            description = "",
            section = antiBanSection
    )
    @Units(Units.PERCENT)
    @Range(min = 0, max = 100)
    default int antiban_mouseoutsidescreen()
    {
        return 0;
    }


    @ConfigItem(
            keyName = "botTimer",
            name = "Bot for (+25%)",
            description = "",
            section = basicSection
    )

    @Units(Units.MINUTES)
    default int botTimer()
    {
        return 30;
    }



    @ConfigItem(
            keyName = "breakTimer",
            name = "Break for (+25%)",
            description = "",
            section = basicSection
    )

    @Units(Units.MINUTES)
    default int breakTimer()
    {
        return 30;
    }


    @ConfigSection(
            name = "Account settings",
            description = "",
            position = 4
    )
    String accountSection = "Account settings";

    @ConfigItem(
            keyName = "username",
            name = "Username",
            description = "",
            position = 5,
            section = accountSection
    )

    default String username()
    {
        return "";
    }

    @ConfigItem(
            keyName = "password",
            name = "Password",
            description = "",
            position = 6,
            section = accountSection
    )

    default String password()
    {
        return "";
    }
}