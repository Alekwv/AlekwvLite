package net.runelite.client.plugins.AlekwvFisher;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

public class AlekwvFisherOverlay extends Overlay
{
	private final Client client;
	private static double angle = 0;
	private final java.util.List<TrailPoint> trailPoints = new ArrayList<>();
	private static final int TRAIL_LIFETIME = 1000;

	@Inject
	AlekwvFisherOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		timer.start();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		long currentTime = System.currentTimeMillis();
		String runTime = formatTime(System.currentTimeMillis() - AlekwvFisherPlugin.startTime);

		Point mP = client.getMouseCanvasPosition();
		trailPoints.add(new TrailPoint(mP, currentTime));
        trailPoints.removeIf(point -> currentTime - point.timestamp > TRAIL_LIFETIME);

		// Draw the trail points as lines
		TrailPoint previousPoint = null;
		for (TrailPoint point : trailPoints)
		{
			if (previousPoint != null)
			{
				int alpha = (int) (255 * (1.0 - (currentTime - point.timestamp) / (double) TRAIL_LIFETIME));
				Color color = new Color(255, 255, 255, alpha);
				graphics.setColor(color);
				graphics.drawLine(previousPoint.position.getX(), previousPoint.position.getY(),
						point.position.getX(), point.position.getY());
			}
			previousPoint = point;
		}

		int circleRadius = 10;
		int xRadius = circleRadius / 3;
		graphics.setColor(Color.WHITE);
		graphics.translate(mP.getX(), mP.getY());
		graphics.rotate(angle);
		graphics.drawOval(-circleRadius, -circleRadius, 2 * circleRadius, 2 * circleRadius);
		graphics.setColor(Color.RED);
		graphics.drawLine(-xRadius, -xRadius, xRadius, xRadius);
		graphics.drawLine(xRadius, -xRadius, -xRadius, xRadius);

		graphics.setTransform(new AffineTransform());

		// Set the color to really dark blue and draw the box
		graphics.setColor(new Color(36, 36, 51));  // RGB for dark blue
		int boxWidth = 100;
		int boxHeight = 75;
		int x = 5;  // X position of the box
		int y = 260;  // Y position of the box
		graphics.fillRect(x, y, boxWidth, boxHeight);

		// Draw the static grey outline
		graphics.setColor(Color.GRAY);
		Stroke oldStroke = graphics.getStroke();
		graphics.setStroke(new BasicStroke(2));  // Set the stroke width for the static outline
		graphics.drawRect(x - 2, y - 2, boxWidth + 4, boxHeight + 4);

		// Draw the moving white outline
		graphics.setColor(Color.WHITE);  // Set the color of the moving outline
		graphics.setStroke(new BasicStroke(2));  // Set the stroke width for the moving outline
		graphics.setStroke(oldStroke);  // Restore the original stroke

		// Set the font for the title text
		Font titleFont = new Font("Arial", Font.BOLD, 14);  // Increase font size for title
		graphics.setFont(titleFont);

		// Draw the title text "Alekwv Fisher" with outlines
		int titleX = x + 2;  // Adjust x position within the box
		int titleY = y + 13;  // Adjust y position to be half inside and half outside the box

		// Draw the outline for Alekwv in black
		graphics.setColor(Color.BLACK);
		graphics.drawString("Alekwv", titleX - 1, titleY - 1);
		graphics.drawString("Alekwv", titleX - 1, titleY + 1);
		graphics.drawString("Alekwv", titleX + 1, titleY - 1);
		graphics.drawString("Alekwv", titleX + 1, titleY + 1);

		// Draw the light blueish Alekwv text
		graphics.setColor(new Color(133, 234, 255));  // Lighter blueish
		graphics.drawString("Alekwv", titleX, titleY);

		// Draw the outline for Fisher in black
		graphics.setColor(Color.BLACK);
		graphics.drawString("Fisher", titleX + 52 - 1, titleY - 1);
		graphics.drawString("Fisher", titleX + 52 - 1, titleY + 1);
		graphics.drawString("Fisher", titleX + 52 + 1, titleY - 1);
		graphics.drawString("Fisher", titleX + 52 + 1, titleY + 1);

		// Draw the white Fisher text
		graphics.setColor(Color.WHITE);
		graphics.drawString("Fisher", titleX + 52, titleY);

		// Set the font for the other text
		Font infoFont = new Font("Arial", Font.PLAIN, 11);
		graphics.setFont(infoFont);

		// Draw the other information inside the box
		graphics.setColor(Color.WHITE);
		int infoStartY = y + 28;  // Starting Y position for the info text
		int infoGap = 12;  // Gap between the info texts

		graphics.drawString("Runtime: " + runTime, x + 4, infoStartY);
		graphics.drawString("Fish caught: " + AlekwvFisherPlugin.fishCaught, x + 4, infoStartY + infoGap);
		graphics.drawString("State: " + AlekwvFisherPlugin.state, x + 4, infoStartY + 2 * infoGap);
		return null;
	}

	private final Timer timer = new Timer(1000 / 60, new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			angle += Math.PI / 150;
		}
	});

	public final String formatTime(final long ms) {
		long s = ms / 1000;
		long m = s / 60;
		long h = m / 60;
		s %= 60;
		m %= 60;
		h %= 24;

		StringBuilder formattedTime = new StringBuilder();

		if (h > 0) {
			formattedTime.append(h).append("h ");
		}
		if (m > 0 || h > 0) {
			formattedTime.append(m).append("m ");
		}
		formattedTime.append(s).append("s");

		return formattedTime.toString().trim();
	}


	private static class TrailPoint
	{
		final Point position;
		final long timestamp;

		TrailPoint(Point position, long timestamp)
		{
			this.position = position;
			this.timestamp = timestamp;
		}
	}
}
