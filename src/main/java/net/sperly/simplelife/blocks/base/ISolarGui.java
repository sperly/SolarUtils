package net.sperly.simplelife.blocks.base;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.sperly.simplelife.SimpleLife;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.ResourceLocation;;

import java.awt.*;
import java.util.ArrayList;

@SideOnly(Side.CLIENT)
public abstract class ISolarGui extends GuiContainer {
    public static final int WIDTH = 180;
    public static final int HEIGHT = 152;

    private ISolarTileEntity te;

    private static ResourceLocation background =  new ResourceLocation(SimpleLife.MODID, "textures/gui/nogui.png");

    public ISolarGui(ISolarTileEntity tileEntity, SolarContainerBase container, String TextureLocation) {
        super(container);
        background = new ResourceLocation(SimpleLife.MODID, TextureLocation);
        te = tileEntity;

        xSize = WIDTH;
        ySize = HEIGHT;
    }

    final int  WORK_BAR_XPOS = 100;
    final int WORK_BAR_YPOS = 45;
    final int WORK_BAR_ICON_U = 180;   // texture position of white arrow icon
    final int WORK_BAR_ICON_V = 0;
    final int WORK_BAR_WIDTH = 16;
    final int WORK_BAR_HEIGHT = 16;

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(background);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        double cookProgress = 0;

            cookProgress = te.fractionOfWorkTimeComplete();


        // draw the cook progress bar
        drawTexturedModalRect(guiLeft +  WORK_BAR_XPOS, guiTop + WORK_BAR_YPOS, WORK_BAR_ICON_U, WORK_BAR_ICON_V,
                (int)(cookProgress * WORK_BAR_WIDTH), WORK_BAR_HEIGHT);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        final int LABEL_XPOS = 5;
        final int LABEL_YPOS = 5;

        this.fontRenderer.drawString(te.getName(), LABEL_XPOS, LABEL_YPOS, Color.darkGray.getRGB());

        java.util.List<String> hoveringText = new ArrayList<String>();

        // If the mouse is over the progress bar add the progress bar hovering text
        if (isInRect(guiLeft + WORK_BAR_XPOS, guiTop + WORK_BAR_YPOS, WORK_BAR_WIDTH, WORK_BAR_HEIGHT, mouseX, mouseY)){
            hoveringText.add("Progress:");
            int cookPercentage =(int)(te.fractionOfWorkTimeComplete() * 100);
            hoveringText.add(cookPercentage + "%");
        }

        // If hoveringText is not empty draw the hovering text
        if (!hoveringText.isEmpty()){
            drawHoveringText(hoveringText, mouseX - guiLeft, mouseY - guiTop, fontRenderer);
        }
//		// You must re bind the texture and reset the colour if you still need to use it after drawing a string
//		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
//		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

    }

    // Returns true if the given x,y coordinates are within the given rectangle
    public static boolean isInRect(int x, int y, int xSize, int ySize, int mouseX, int mouseY){
        return ((mouseX >= x && mouseX <= x+xSize) && (mouseY >= y && mouseY <= y+ySize));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        super.renderHoveredToolTip(mouseX, mouseY);
    }
}
