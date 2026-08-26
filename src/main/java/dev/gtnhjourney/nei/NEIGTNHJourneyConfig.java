package dev.gtnhjourney.nei;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.guihook.GuiContainerManager;
import cpw.mods.fml.common.FMLCommonHandler;
import dev.gtnhjourney.GTNHJourney;

/** NEI discovers this class client-side by its NEI*Config naming convention. */
public final class NEIGTNHJourneyConfig implements IConfigureNEI {

    private static final RegistrationGate REGISTRATION = new RegistrationGate();

    @Override
    public void loadConfig() {
        if (!REGISTRATION.acquire()) return;
        API.addSubset("Journey.Researched", new JourneySubsetFilter());
        API.addSubset("Journey.Newest", new JourneyNewestFilter());
        API.addItemFilter(new JourneyGlobalSafetyFilter());
        API.addItemFilter(new JourneyItemFilterProvider());
        API.registerNEIGuiHandler(new JourneyCreativeGuiHandler());
        // Must run before NEI LayoutManager, which otherwise consumes item-panel clicks first.
        GuiContainerManager.inputHandlers.addFirst(new JourneyNEIInputHandler());
        GuiContainerManager.addTooltipHandler(new JourneyNEITooltipHandler());
        JourneyNEIToggleWidget toggle = new JourneyNEIToggleWidget();
        GuiContainerManager.addDrawHandler(toggle);
        GuiContainerManager.addInputHandler(toggle);
        GuiContainerManager.addTooltipHandler(toggle);
        FMLCommonHandler.instance()
            .bus()
            .register(new JourneyNEIRefreshTracker());
    }

    @Override
    public String getName() {
        return GTNHJourney.NAME + " NEI Integration";
    }

    @Override
    public String getVersion() {
        return GTNHJourney.VERSION;
    }
}
