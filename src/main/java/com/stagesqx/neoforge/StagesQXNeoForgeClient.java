package com.stagesqx.neoforge;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.function.Predicate;

public final class StagesQXNeoForgeClient {
	private static final StackWalker JEI_RENDER_STACK = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

	private StagesQXNeoForgeClient() {
	}

	static void init(IEventBus modBus) {
		modBus.addListener(StagesQXNeoForgeClient::onRegisterDecorations);
		NeoForge.EVENT_BUS.addListener(StagesQXNeoForgeClient::onItemTooltip);
	}

	private static void onRegisterDecorations(RegisterItemDecorationsEvent event) {
		LockedItemDecorator dec = new LockedItemDecorator();
		net.minecraft.core.registries.BuiltInRegistries.ITEM.forEach(item -> event.register(item, dec));
	}

	private static void onItemTooltip(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		if (stack.isEmpty()) {
			return;
		}
		if (!StageClientView.isBlocked(stack)) {
			return;
		}
		event.getToolTip().add(Component.translatable("tooltip.stagesqx.blocked.title").withStyle(ChatFormatting.RED));
		if (StageDisclosure.mayShowRestrictingStageNameClient()) {
			event.getToolTip().add(Component.translatable("tooltip.stagesqx.blocked.stage", StageClientView.blockingStageName(stack)));
		} else {
			event.getToolTip().add(Component.translatable("tooltip.stagesqx.blocked.hint_generic").withStyle(ChatFormatting.GRAY));
		}
	}

	private static final class LockedItemDecorator implements IItemDecorator {
		@Override
		public boolean render(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
			if (!StageClientView.isBlocked(stack)) {
				return false;
			}
			if (stackWalkAny(JEI_RENDER_STACK, f -> {
				String n = f.getClassName();
				return n.startsWith("mezz.jei.");
			})) {
				return false;
			}
			// Same Z lift as vanilla stack count in renderItemDecorations — item mesh is drawn at ~150, we must draw above it.
			graphics.pose().pushPose();
			graphics.pose().translate(0.0F, 0.0F, 200.0F);
			graphics.fill(RenderType.guiOverlay(), x, y, x + 16, y + 16, 0x80484848);
			String lock = "\uD83D\uDD12";
			int tw = font.width(lock);
			int lockX = x + (16 - tw) / 2;
			int lockY = y + (16 - font.lineHeight) / 2;
			graphics.drawString(font, lock, lockX, lockY, 0xFFFFFFFF, true);
			graphics.pose().popPose();
			return true;
		}
	}

	private static boolean stackWalkAny(StackWalker walker, Predicate<StackWalker.StackFrame> test) {
		return walker.walk(s -> s.limit(32).anyMatch(test));
	}
}
