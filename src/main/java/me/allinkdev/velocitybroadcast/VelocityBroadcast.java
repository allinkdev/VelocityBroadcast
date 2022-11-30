package me.allinkdev.velocitybroadcast;

import com.google.inject.Inject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.Scheduler;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.slf4j.Logger;

@Plugin(
	id = "velocitybroadcast",
	name = "VelocityBroadcast",
	version = BuildConstants.VERSION
)
public class VelocityBroadcast {

	private static final Duration TEN_MINUTES = Duration.of(10, ChronoUnit.MINUTES);
	private static final Duration NEVER = Duration.ZERO;
	private static final Component SYSTEM_ALERT_TITLE = Component.empty()
		.append(Component.text("[", NamedTextColor.DARK_GRAY))
		.append(Component.text("SYSTEM ALERT", NamedTextColor.DARK_RED))
		.append(Component.text("]", NamedTextColor.DARK_GRAY));
	@Inject
	private Logger logger;

	@Inject
	private ProxyServer proxyServer;
	private Scheduler proxyScheduler;

	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {
		final CommandManager commandManager = proxyServer.getCommandManager();

		commandManager.register(
			new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("alert")
				.requires(s -> s.hasPermission("broadcast.command.alert"))
				.then(RequiredArgumentBuilder.<CommandSource, String>argument("message",
						StringArgumentType.greedyString())
					.executes(this::execute)
				)
				.build()
			));

		this.proxyScheduler = proxyServer.getScheduler();
	}

	private void displayMessage(Player player, String message) {
		displayMessage(player, Component.text(message));
	}

	private void displayMessage(Player player, Component message) {
		final Component formattedMessage = message.color(NamedTextColor.GRAY);
		final Component chatMessage = SYSTEM_ALERT_TITLE.append(Component.space())
			.append(formattedMessage);

		player.sendMessage(chatMessage);

		final Times times = Times.of(NEVER, TEN_MINUTES, NEVER);
		final Title title = Title.title(SYSTEM_ALERT_TITLE, formattedMessage, times);
		player.showTitle(title);

		proxyScheduler.buildTask(this, player::resetTitle)
			.delay(30, TimeUnit.SECONDS)
			.schedule();
	}

	private int execute(CommandContext<CommandSource> ctx) {
		final String message = StringArgumentType.getString(ctx, "message");
		final Collection<Player> players = proxyServer.getAllPlayers();

		for (Player player : players) {
			this.displayMessage(player, message);
		}

		final CommandSource source = ctx.getSource();
		final String sourceName =
			(source instanceof final Player player) ? player.getUsername() : "CONSOLE";

		logger.info("[{}] [SYSTEM ALERT] {}", sourceName, message);

		return 1;
	}
}
