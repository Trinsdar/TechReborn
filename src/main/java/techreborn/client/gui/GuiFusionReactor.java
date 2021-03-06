/*
 * This file is part of TechReborn, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2018 TechReborn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package techreborn.client.gui;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;
import reborncore.client.multiblock.Multiblock;
import reborncore.client.multiblock.MultiblockRenderEvent;
import reborncore.client.multiblock.MultiblockSet;
import reborncore.common.network.NetworkManager;
import reborncore.common.powerSystem.PowerSystem;
import reborncore.common.util.Torus;
import techreborn.client.gui.widget.GuiButtonHologram;
import techreborn.client.gui.widget.GuiButtonUpDown;
import techreborn.init.ModBlocks;
import techreborn.packets.PacketFusionControlSize;
import techreborn.proxies.ClientProxy;
import techreborn.tiles.fusionReactor.TileFusionControlComputer;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class GuiFusionReactor extends GuiBase {
	TileFusionControlComputer tile;

	public GuiFusionReactor(final EntityPlayer player, final TileFusionControlComputer tile) {
		super(player, tile, tile.createContainer(player));
		this.tile = tile;
	}

	@Override
	public void initGui() {
		super.initGui();

	}

	@Override
	protected void drawGuiContainerBackgroundLayer(final float partialTicks, final int mouseX, final int mouseY) {
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		final GuiBase.Layer layer = GuiBase.Layer.BACKGROUND;

		this.drawSlot(34, 47, layer);
		this.drawSlot(126, 47, layer);
		this.drawOutputSlot(80, 47, layer);

		this.builder.drawJEIButton(this, 150, 4, layer);

	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(final int mouseX, final int mouseY) {
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
		final GuiBase.Layer layer = GuiBase.Layer.FOREGROUND;

		this.builder.drawProgressBar(this, this.tile.getProgressScaled(100), 100, 55, 51, mouseX, mouseY, TRBuilder.ProgressDirection.RIGHT, layer);
		this.builder.drawProgressBar(this, this.tile.getProgressScaled(100), 100, 105, 51, mouseX, mouseY, TRBuilder.ProgressDirection.LEFT, layer);
		if (tile.getCoilStatus() > 0) {
			addHologramButton(6, 4, 212, layer);
			builder.drawHologramButton(this, 6, 4, mouseX, mouseY, layer);
			drawCentredString(tile.getStateString(), 20, Color.BLUE.darker().getRGB(), layer);
			if(tile.state == 2){
				drawCentredString( PowerSystem.getLocaliszedPowerFormatted((int) tile.getPowerChange()) + "/t", 30, Color.GREEN.darker().getRGB(), layer);
			}
		} else {
			builder.drawMultiblockMissingBar(this, layer);
			addHologramButton(76, 56, 212, layer);
			builder.drawHologramButton(this, 76, 56, mouseX, mouseY, layer);

			Optional<Pair<Integer, Integer>> stackSize = getCoilStackCount();
			if(stackSize.isPresent()){
				if(stackSize.get().getLeft() > 0){
					drawCentredString("Required Coils: " + stackSize.get().getLeft() + "x64 +" + stackSize.get().getRight(), 25, 0xFFFFFF,  layer);
				} else {
					drawCentredString("Required Coils: " + stackSize.get().getRight(), 25, 0xFFFFFF, layer);
				}

			}
		}
		this.builder.drawUpDownButtons(this, 121, 79, layer);
		drawString("Size: " + tile.size, 83, 81, 0xFFFFFF, layer);
		drawString("" + tile.getPowerMultiplier() + "x", 10, 81, 0xFFFFFF, layer);

		buttonList.add(new GuiButtonUpDown(300, 121, 79, this, GuiBase.Layer.FOREGROUND));
		buttonList.add(new GuiButtonUpDown(301, 121 + 12, 79, this, GuiBase.Layer.FOREGROUND));
		buttonList.add(new GuiButtonUpDown(302, 121 + 24, 79, this, GuiBase.Layer.FOREGROUND));
		buttonList.add(new GuiButtonUpDown(303, 121 + 36, 79, this, GuiBase.Layer.FOREGROUND));

		this.builder.drawMultiEnergyBar(this, 9, 19, (int) this.tile.getEnergy(), (int) this.tile.getMaxPower(), mouseX, mouseY, 0, layer);
	}
	
	public void addHologramButton(int x, int y, int id, Layer layer) {
		int factorX = 0;
		int factorY = 0;
		if (layer == Layer.BACKGROUND) {
			factorX = guiLeft;
			factorY = guiTop;
		}
		buttonList.add(new GuiButtonHologram(id, x + factorX, y + factorY, this, layer));
	}
	
	@Override
	public void actionPerformed(final GuiButton button) throws IOException {
		super.actionPerformed(button);
		if (button.id == 212 && GuiBase.slotConfigType == SlotConfigType.NONE) {
			if (ClientProxy.multiblockRenderEvent.currentMultiblock == null) {
				updateMultiBlockRender();
			} else {
				ClientProxy.multiblockRenderEvent.setMultiblock(null);
			}
		}
		if (button.id == 300){
			sendSizeChange(5);
		} else if (button.id == 301){
			sendSizeChange(1);
		} else if (button.id == 302){
			sendSizeChange(-1);
		} else if (button.id == 303){
			sendSizeChange(-5);
		}
	}

	private void sendSizeChange(int sizeDelta){
		NetworkManager.sendToServer(new PacketFusionControlSize(sizeDelta, tile.getPos()));
		//Reset the multiblock as it will be wrong now.
		if(ClientProxy.multiblockRenderEvent.currentMultiblock != null){
			updateMultiBlockRender();
		}
	}

	private void updateMultiBlockRender(){
		final Multiblock multiblock = new Multiblock();
		IBlockState coil = ModBlocks.FUSION_COIL.getDefaultState();

		List<BlockPos> coils = Torus.generate(new BlockPos(0, 0, 0), tile.size);
		coils.forEach(pos -> addComponent(pos.getX(), pos.getY(), pos.getZ(), coil, multiblock));

		final MultiblockSet set = new MultiblockSet(multiblock);
		ClientProxy.multiblockRenderEvent.setMultiblock(set);
		ClientProxy.multiblockRenderEvent.parent = this.tile.getPos();
		MultiblockRenderEvent.anchor = new BlockPos(this.tile.getPos().getX(), this.tile.getPos().getY() - 1,
			this.tile.getPos().getZ());
	}
	
	public void addComponent(final int x, final int y, final int z, final IBlockState blockState, final Multiblock multiblock) {
		multiblock.addComponent(new BlockPos(x, y, z), blockState);
	}

	public Optional<Pair<Integer, Integer>> getCoilStackCount(){
		if(!Torus.TORUS_SIZE_MAP.containsKey(tile.size)){
			return Optional.empty();
		}
		int count = Torus.TORUS_SIZE_MAP.get(tile.size);
		return Optional.of(Pair.of(count / 64, count % 64));
	}
}
