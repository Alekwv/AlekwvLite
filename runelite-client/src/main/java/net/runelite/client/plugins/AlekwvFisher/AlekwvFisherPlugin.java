package net.runelite.client.plugins.AlekwvFisher;

import com.google.inject.Provides;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.WorldService;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.naturalmouse.api.MouseMotionFactory;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.WorldResult;
import javax.inject.Inject;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@PluginDescriptor(
        name = "Alekwv Fisher",
        description = "",
        tags = {},
        enabledByDefault = true
)

public class AlekwvFisherPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ClientThread clientThread;

    @Inject
    private MouseManager mouseManager;

    @Inject
    private WorldService worldService;

    @Inject
    private KeyManager keyManager;

    @Inject
    private AlekwvFisherOverlay overlay;

    @Inject
    private AlekwvFisherInputListener inputListener;

    @Inject
    private AlekwvFisherConfig config;

    ExecutorService executor;
    Robot robot;
    WinDef.HWND hwnd;
    Future<?> MouseTask;
    Thread shiftThread;
    Rectangle windowPosition;
    Dimension screenSize;
    WorldPoint lastPosition;

    static public String state;
    int[] equipmentIDs, droppableIDs;
    public static int fishCaught = 0;
    int npcId = 0;
    public static boolean holdShift = false;
    boolean wasHoldingShift = false;
    static public long lastInteraction, lastAnimation, startTime;
    long lastWindowCheck;
    boolean forceContinue;


    @Provides
    AlekwvFisherConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AlekwvFisherConfig.class);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) throws InterruptedException {
        if (event.getKey().equals("world")) {
            applyWorld();
        } else if (event.getKey().equals("username") || event.getKey().equals("password")) {
            applyCredentials();
        } else {
            checkCfg();
        }
    }

    @Override
    protected void startUp() throws Exception {
        fishCaught = 0;
        startTime = System.currentTimeMillis();
        state = "Fish";
        checkCfg();
        overlayManager.add(overlay);
        mouseManager.registerMouseListener(inputListener);
        keyManager.registerKeyListener(inputListener);
        robot = new Robot();
        executor = Executors.newSingleThreadExecutor();
        client.setUsername("aleknikkanen+1def@gmail.com");
        client.setPassword("eidefua100801");
        applyWorld();
        applyCredentials();

        clientThread.invoke(() ->
        {
            applyWorld();
            if (client.getGameState().getState() >= GameState.LOGIN_SCREEN.getState()) {
                client.changeMemoryMode(true);
            };
        });

        Thread shiftThread = new Thread(() -> {
            while (true) {
                try {
                    if (holdShift) {
                        robot.keyPress(KeyEvent.VK_SHIFT);
                        wasHoldingShift = true;
                    } else {
                        if (wasHoldingShift) {
                            robot.keyRelease(KeyEvent.VK_SHIFT);
                            wasHoldingShift = false;
                        }
                    }
                    Thread.sleep(getRandomNumber(15, 30));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        shiftThread.start();
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(overlay);
        mouseManager.unregisterMouseListener(inputListener);
        keyManager.unregisterKeyListener(inputListener);
        clientThread.invoke(() -> client.changeMemoryMode(false));
        executor.shutdownNow();
        shiftThread.interrupt();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) throws InterruptedException {
        if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN) {
            Login();
            client.changeMemoryMode(true);
        } else if (gameStateChanged.getGameState() == GameState.STARTING) {
            client.changeMemoryMode(false);
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer != null) {
            WorldPoint currentPosition = localPlayer.getWorldLocation();
            if (!currentPosition.equals(lastPosition)) {
                lastPosition = currentPosition;
                lastAnimation = System.currentTimeMillis();
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() == ChatMessageType.GAMEMESSAGE || event.getType() == ChatMessageType.SPAM)
        {
            String message = event.getMessage();
            if (message.contains("You catch"))
            {
                fishCaught++;
            }
        }
    }


    @Subscribe
    public void onClientTick(ClientTick event) throws InterruptedException {

        if (!RunBasicChecks()) {
            return;
        }

        if (isInventoryFull() || state.equals("Drop")) {
            state = "Drop";
            dropItems(droppableIDs);
            forceContinue = true;
        } else if (state.equals("Fish")) {

            if (!forceContinue && !LastInteraction(1200)) {
                return;
            }

            NPC fishingSpot = getClosestNPC(npcId);
            if (fishingSpot != null) {
                Log("We fishing! forceContinue = " + forceContinue);
                MouseTask = MouseMove(fishingSpot.getConvexHull().getBounds(), "left", 0, 0, true);
                forceContinue = false;
            }
        }
    }
    public void MoveMouseOutsideScreen() throws InterruptedException {
        if (isMouseOutsideScreen()) {
            return;
        }

        WinUser.WINDOWINFO winInfo = new WinUser.WINDOWINFO();
        User32.INSTANCE.GetWindowInfo(hwnd, winInfo);
        int x = winInfo.rcWindow.left;
        int y = winInfo.rcWindow.top;
        int w = winInfo.rcWindow.bottom;
        int h = winInfo.rcWindow.right;
        int randomCorner = getRandomNumber(1, 2);
        if (randomCorner == 1) {
            MouseMove(getRandomNumber(x, h), w + getRandomNumber(10, 30), "false", getRandomNumber(0, 1800),  0, false);
        } else if (randomCorner == 2) {
            MouseMove(h + getRandomNumber(10, 30), getRandomNumber(y, w), "false", getRandomNumber(0, 1800),  0, false);
        }
    }


    void checkCfg()
    {
        if (config.location() == AlekwvFisherConfig.locations.NONE) {
            equipmentIDs = new int[]{0};
            droppableIDs = new int []{0};
            npcId = 0;
        } else if (config.location() == AlekwvFisherConfig.locations.LUMBRIDGE_SWAMP) {
            equipmentIDs = new int[]{303};
            droppableIDs = new int []{317, 321};
            npcId = 1520;
        } else if (config.location() == AlekwvFisherConfig.locations.BARBARIAN_VILLAGE) {
            equipmentIDs = new int[]{309, 314};
            droppableIDs = new int []{331, 335};
            npcId = 1526;
        }
    }

    boolean RunBasicChecks() throws InterruptedException {

        if (System.currentTimeMillis() - lastWindowCheck > 600)
        {
            lastWindowCheck = System.currentTimeMillis();
            if (hwnd == null) {
                hwnd = User32.INSTANCE.FindWindow(null, "RuneLite");
                Log("Searching for RuneLite window");
                return false;
            }

            WinUser.WINDOWINFO winInfo = new WinUser.WINDOWINFO();
            User32.INSTANCE.GetWindowInfo(hwnd, winInfo);
            windowPosition = winInfo.rcClient.toRectangle();
            screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        }

        if (MouseTask != null && !MouseTask.isDone()) {
            return false;
        }

        if (client.getGameState() == GameState.LOGIN_SCREEN) {
            Login();
            return false;
        }

        if (client.getGameState() == GameState.LOGGED_IN)
        {
            Player localPlayer = client.getLocalPlayer();
            if (!String.valueOf(localPlayer.getInteracting()).equals("null")) {
                lastInteraction = System.currentTimeMillis();
            }

            if (localPlayer.getAnimation() != -1) {
                lastAnimation = System.currentTimeMillis();
            }

            Widget loginScreenButton = client.getWidget(378, 72);
            if (loginScreenButton != null && !loginScreenButton.isHidden())
            {
                if (LastInteraction(1200)) {
                    MouseTask = MouseMove(loginScreenButton.getBounds(), "left", 0, 0, false);
                }
                return false;
            }

            if (client.getScale() > 250) {
                Widget cameraZoomTick = client.getWidget(116, 49);
                if (cameraZoomTick != null && !cameraZoomTick.isHidden())
                {
                    if (LastInteraction(1200)) {
                        MouseMove(cameraZoomTick.getBounds(), "left", 0, 0, false);
                    }
                }
                else
                {
                    Widget allSettingsButton = client.getWidget(116, 32);
                    if (allSettingsButton != null && !allSettingsButton.isHidden())
                    {
                        Widget displaySettings = client.getWidget(116, 69);
                        if (displaySettings != null) {
                            if (LastInteraction(1200)) {
                                MouseTask = MouseMove(displaySettings.getBounds(), "left", 0, 0, false);
                            }
                        }
                    }
                    else
                    {
                        if (LastInteraction(1200)) {
                            MouseTask = KeyboardPress(KeyEvent.VK_F10);
                        }
                    }
                }
                return false;
            }
        }

        return true;
    }


    public NPC getClosestNPC(int id) {
        NPC[] cachedNPCs = client.getCachedNPCs();
        NPC closestNPC = null;
        double closestDistance = Double.MAX_VALUE;
        LocalPoint playerLocation = client.getLocalPlayer().getLocalLocation();
        for (NPC npc : cachedNPCs) {
            if (npc != null && npc.getId() == id) {
                LocalPoint npcLocation = npc.getLocalLocation();
                if (npcLocation != null) {
                    int dx = playerLocation.getX() - npcLocation.getX();
                    int dy = playerLocation.getY() - npcLocation.getY();
                    double distanceSq = dx * dx + dy * dy;
                    if (distanceSq < closestDistance) {
                        closestDistance = distanceSq;
                        closestNPC = npc;
                    }
                }
            }
        }
        return closestNPC;
    }

    public boolean LastInteraction(long time) {
        return System.currentTimeMillis() - lastInteraction > time;
    }

    public boolean isInventoryFull()
    {
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        if (inventory != null) {
            return inventory.count() == 28;
        }
        return false;
    }

    public boolean inventoryContains(int id) {
        return inventoryContains(new int[] {id});
    }

    public boolean inventoryContains(int[] ids) {
        ItemContainer container = client.getItemContainer(InventoryID.INVENTORY);
        if (container != null) {
            for (Item item : container.getItems()) {
                int itemId = item.getId();
                for (int id : ids) {
                    if (itemId == id) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void dropItems(int[] ids) throws InterruptedException {
        if (inventoryContains(ids)) {
            if (isMouseOutsideScreen()) {
                activateWindow();
            }

            Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
            if (inventoryWidget != null && !inventoryWidget.isHidden()) {
                holdShift = true;
                for (Widget slot : inventoryWidget.getChildren()) {

                    //skip empty
                    if (slot.getId() == 6512) {
                        continue;
                    }

                    for (int id : ids) {
                        if (id == slot.getItemId()) {
                            try {
                                MouseTask = MouseMove(slot.getBounds(), "left", 0, 0, false);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } else {
                KeyboardPress(KeyEvent.VK_ESCAPE);
                Log("invy not open");
            }
        }
        else
        {
            holdShift = false;
            state = "Fish";
        }
    }

    public void Antiban(String mode) throws InterruptedException {

        int[] antibanMode = Arrays.stream(mode.split(",")).mapToInt(Integer::parseInt).toArray();
        int antibanChance = getRandomNumber(0, 100);
        List<Runnable> antibanActions = new ArrayList<>();


        //1
        antibanActions.add(() -> {
            if (Arrays.stream(antibanMode).anyMatch(n -> n == 1) && config.antibanvalue_mousemovement() < antibanChance) {
                Log("antibanvalue_mousemovement");
                for (int i = 1; i < 3; i++) {
                    if (!isMouseOutsideScreen()) {
                        MouseTask = MouseMove(new Rectangle(1, 1, 764, 502), "false", 0, getRandomNumber(50, 500), false);
                    }
                }
            }
        });


        //2
        antibanActions.add(() -> {
            if (isMouseOutsideScreen()) {
                return;
            }
            if (Arrays.stream(antibanMode).anyMatch(n -> n == 2) && config.antibanvalue_cameramovement() > antibanChance) {
                Log("antibanvalue_cameramovement");
            }
        });

        //3
        antibanActions.add(() -> {

            if (isMouseOutsideScreen()) {
                return;
            }

            if (Arrays.stream(antibanMode).anyMatch(n -> n == 3) && config.antibanvalue_clickmouse() > antibanChance) {
                Log("antibanvalue_clickmouse");
                for (int i = 0; i < getRandomNumber(1, 2); i++) {
                    MouseTask = MouseMove(new Rectangle(1, 1, 764, 502), getRandomNumber(1, 2) == 1 ? "middle" : "right", 0, getRandomNumber(50, 500), false);
                }
            }
        });

        //4
        antibanActions.add(() -> {
            if (Arrays.stream(antibanMode).anyMatch(n -> n == 4) && config.antiban_mouseoutsidescreen() > antibanChance) {
                try {
                    Log("antiban_mouseoutsidescreen");
                    Thread.sleep(getRandomNumber(0, 2000));
                    MoveMouseOutsideScreen();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // Shuffle the actions
        Collections.shuffle(antibanActions);

        // Execute each action
        for (Runnable action : antibanActions) {
            action.run();
        }
    }

    public void dropItems(int id) throws InterruptedException {
        dropItems(new int[]{id});
    }

    private void applyCredentials() {
        if (client.getGameState() == GameState.LOGIN_SCREEN) {
            client.setUsername(config.username());
            client.setPassword(config.password());
        }
    }

    public void activateWindow() {
        Rectangle gameArea = new Rectangle(1, 1, 525, 460);
        int mode = getRandomNumber(1, 2);
        if (mode == 1) {
            MouseTask = MouseMove(gameArea, "right", 0, 0, false);
        } else {
            MouseTask = MouseMove(gameArea, "middle", 0, 0, false);
        }
    }

    public boolean isMouseOutsideScreen() {
        Point mp = client.getMouseCanvasPosition();
        return (mp.getX() == -1 || mp.getY() == -1);
    }

    public void Login() throws InterruptedException {
        state = "Login";
        applyWorld();
        applyCredentials();
    }

    public Future<?> KeyboardPress(int vKey) {
        lastAnimation = System.currentTimeMillis();
        return executor.submit(() -> {
            try {
                if (isMouseOutsideScreen()) {
                    activateWindow();
                }
                robot.keyPress(vKey);
                Thread.sleep(getRandomNumber(25, 150));
                robot.keyRelease(vKey);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public void MouseMove(int x, int y, String click, int sleepBefore, int sleepAfter, boolean antiban) {
        MouseTask = MouseMove(new Rectangle(x, y, 0, 0), click, sleepBefore, sleepAfter, antiban);
    }

    public Future<?> MouseMove(Rectangle bounds, String click, int sleepBefore, int sleepAfter, boolean antiban) {
        return executor.submit(() -> {
            try {

                if (isMouseOutsideScreen()) {
                    Thread.sleep(getRandomNumber(0, 7000));
                    Antiban("1,2,3");
                }

                Thread.sleep(sleepBefore);
                Rectangle newBounds = GetCenterFromRectangle(bounds);
                int fixedX = windowPosition.x + newBounds.x;
                int fixedY = windowPosition.y + newBounds.y;
                int randomX = getRandomNumber(fixedX, fixedX + newBounds.width);
                int randomY = getRandomNumber(fixedY, fixedY + newBounds.height);
                MouseMotionFactory.getDefault().move(randomX, randomY);
                Thread.sleep(getRandomNumber(0, 25));
                if (!click.equals("false")) {
                    int button = 0;
                    switch (click) {
                        case "left":
                            button = InputEvent.BUTTON1_DOWN_MASK;
                            break;
                        case "middle":
                            button = InputEvent.BUTTON2_DOWN_MASK;
                            break;
                        case "right":
                            button = InputEvent.BUTTON3_DOWN_MASK;
                            break;
                    }
                    robot.mousePress(button);
                    Thread.sleep(getRandomNumber(20, 125));
                    robot.mouseRelease(button);
                }
                Thread.sleep(sleepAfter);
                if (antiban) {
                    Antiban("1,2,3,4");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public static void Log(String text) {
        DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(TIME_FORMATTER);
        String brightGreenColor = "\u001B[92m";
        String resetColor = "\u001B[0m";
        System.out.println(brightGreenColor + "[" + time + "] " + text + resetColor);
    }

    public static void Log(int text) {
        Log(String.valueOf(text));
    }

    public static int getRandomNumber(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }

    private void applyWorld()
    {
        if (client.getGameState() != GameState.LOGIN_SCREEN || config.getWorld() < 301 || client.getWorld() == config.getWorld())
        {
            return;
        }

        final WorldResult worldResult = worldService.getWorlds();
        if (worldResult == null)
        {
            return;
        }

        final net.runelite.http.api.worlds.World world = worldResult.findWorld(config.getWorld());
        if (world == null)
        {
            System.out.println("World not found." + config.getWorld());
            return;
        }

        final World rsWorld = client.createWorld();
        rsWorld.setActivity(world.getActivity());
        rsWorld.setAddress(world.getAddress());
        rsWorld.setId(world.getId());
        rsWorld.setPlayerCount(world.getPlayers());
        rsWorld.setLocation(world.getLocation());
        rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));
        client.changeWorld(rsWorld);
    }

    Rectangle GetCenterFromRectangle(Rectangle rectangle) {
        Rectangle originalBounds = rectangle.getBounds();

        // Calculate the center point of the original rectangle
        int centerX = originalBounds.x + originalBounds.width / 2;
        int centerY = originalBounds.y + originalBounds.height / 2;

        // Scale factor for the new rectangle (1.5 times smaller)
        double scaleFactor = 1.0 / 1.5;

        // Calculate the new rectangle's dimensions
        int newWidth = (int) (originalBounds.width * scaleFactor);
        int newHeight = (int) (originalBounds.height * scaleFactor);

        // Calculate the new rectangle's top-left corner
        int newX = centerX - newWidth / 2;
        int newY = centerY - newHeight / 2;

        // Draw the new rectangle
        return new Rectangle(newX, newY, newWidth, newHeight);
    }
}
