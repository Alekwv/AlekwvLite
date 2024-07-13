package net.runelite.client.plugins.AlekwvFisher;

import net.runelite.api.Client;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.MouseAdapter;
import javax.inject.Inject;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class AlekwvFisherInputListener extends MouseAdapter implements KeyListener
{
    private final Client client;
    private final AlekwvFisherPlugin plugin;

    @Inject
    private AlekwvFisherInputListener(Client client, AlekwvFisherPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
    }

    @Override
    public MouseEvent mouseClicked(MouseEvent e)
    {
        AlekwvFisherPlugin.lastInteraction = System.currentTimeMillis();
        return super.mouseClicked(e);
    }

    @Override
    public MouseEvent mouseMoved(MouseEvent e)
    {
        AlekwvFisherPlugin.lastInteraction = System.currentTimeMillis();
        return super.mouseMoved(e);
    }

    @Override
    public void keyTyped(KeyEvent e)
    {

    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        AlekwvFisherPlugin.lastInteraction = System.currentTimeMillis();
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        AlekwvFisherPlugin.lastInteraction = System.currentTimeMillis();
    }
}