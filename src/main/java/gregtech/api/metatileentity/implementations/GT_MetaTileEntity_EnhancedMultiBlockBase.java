package gregtech.api.metatileentity.implementations;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.IAlignmentProvider;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.alignment.enumerable.Rotation;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import cpw.mods.fml.common.network.NetworkRegistry;
import gregtech.api.GregTech_API;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GT_Multiblock_Tooltip_Builder;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.input.Keyboard;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Enhanced multiblock base class, featuring following improvement over {@link GT_MetaTileEntity_MultiBlockBase}
 * <p>
 * 1. TecTech style declarative structure check utilizing StructureLib.
 * 2. Arbitrarily rotating the whole structure, if allowed to.
 *
 * @param <T> type of this
 */
public abstract class GT_MetaTileEntity_EnhancedMultiBlockBase<T extends GT_MetaTileEntity_EnhancedMultiBlockBase<T>> extends GT_MetaTileEntity_MultiBlockBase implements IAlignment, IConstructable {
	private static final AtomicReferenceArray<GT_Multiblock_Tooltip_Builder> tooltips = new AtomicReferenceArray<>(GregTech_API.METATILEENTITIES.length);
	private ExtendedFacing mExtendedFacing = ExtendedFacing.DEFAULT;
	private final IAlignmentLimits mLimits = getInitialAlignmentLimits();

	protected GT_MetaTileEntity_EnhancedMultiBlockBase(int aID, String aName, String aNameRegional) {
		super(aID, aName, aNameRegional);
	}

	protected GT_MetaTileEntity_EnhancedMultiBlockBase(String aName) {
		super(aName);
	}

	@Override
	public ExtendedFacing getExtendedFacing() {
		return mExtendedFacing;
	}

	@Override
	public void setExtendedFacing(ExtendedFacing newExtendedFacing) {
		if (mExtendedFacing != newExtendedFacing) {
			stopMachine();
			mExtendedFacing = newExtendedFacing;
			IGregTechTileEntity base = getBaseMetaTileEntity();
			mMachine = false;
			mUpdate = 100;
			if (getBaseMetaTileEntity().isServerSide()) {
				StructureLibAPI.sendAlignment((IAlignmentProvider) base,
						new NetworkRegistry.TargetPoint(base.getWorld().provider.dimensionId,
								base.getXCoord(), base.getYCoord(), base.getZCoord(), 512));
			} else {
				base.issueTextureUpdate();
			}
		}
	}

	@Override
	public final boolean isFacingValid(byte aFacing) {
		return canSetToDirectionAny(ForgeDirection.getOrientation(aFacing));
	}

	@Override
	public void onFacingChange() {
		toolSetDirection(ForgeDirection.getOrientation(getBaseMetaTileEntity().getFrontFacing()));
	}

	@Override
	public IAlignmentLimits getAlignmentLimits() {
		return mLimits;
	}

	public abstract IStructureDefinition<T> getStructureDefinition();

	protected abstract GT_Multiblock_Tooltip_Builder createTooltip();

	@Override
	public String[] getDescription() {
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
			return getTooltip().getStructureInformation();
		} else {
			return getTooltip().getInformation();
		}
	}

	protected GT_Multiblock_Tooltip_Builder getTooltip() {
		int tId = getBaseMetaTileEntity().getMetaTileID();
		GT_Multiblock_Tooltip_Builder tooltip = tooltips.get(tId);
		if (tooltip == null) {
			tooltip = createTooltip();
			tooltips.set(tId, tooltip);
		}
		return tooltip;
	}

	@Override
	public String[] getStructureDescription(ItemStack stackSize) {
		return getTooltip().getStructureInformation();
	}

	protected IAlignmentLimits getInitialAlignmentLimits() {
		return UNLIMITED;
	}

	@Override
	public void saveNBTData(NBTTagCompound aNBT) {
		super.saveNBTData(aNBT);
		aNBT.setByte("mRotation", (byte) mExtendedFacing.getRotation().getIndex());
		aNBT.setByte("mFlip", (byte) mExtendedFacing.getFlip().getIndex());
	}

	@Override
	public void loadNBTData(NBTTagCompound aNBT) {
		super.loadNBTData(aNBT);
		mExtendedFacing = ExtendedFacing.of(ForgeDirection.getOrientation(getBaseMetaTileEntity().getFrontFacing()),
				Rotation.byIndex(aNBT.getByte("mRotation")),
				Flip.byIndex(aNBT.getByte("mFlip")));
	}

	@SuppressWarnings("unchecked")
	private IStructureDefinition<GT_MetaTileEntity_EnhancedMultiBlockBase<T>> getCastedStructureDefinition() {
		return (IStructureDefinition<GT_MetaTileEntity_EnhancedMultiBlockBase<T>>) getStructureDefinition();
	}

	protected final boolean checkPiece(String piece, int horizontalOffset, int verticalOffset, int depthOffset) {
		IGregTechTileEntity tTile = getBaseMetaTileEntity();
		return getCastedStructureDefinition().check(this, piece, tTile.getWorld(), getExtendedFacing(), tTile.getXCoord(), tTile.getYCoord(), tTile.getZCoord(), horizontalOffset, verticalOffset, depthOffset, !mMachine);
	}

	protected final boolean buildPiece(String piece, ItemStack trigger, boolean hintOnly, int horizontalOffset, int verticalOffset, int depthOffset) {
		IGregTechTileEntity tTile = getBaseMetaTileEntity();
		return getCastedStructureDefinition().buildOrHints(this, trigger, piece, tTile.getWorld(), getExtendedFacing(), tTile.getXCoord(), tTile.getYCoord(), tTile.getZCoord(), horizontalOffset, verticalOffset, depthOffset, hintOnly);
	}

	@Override
	public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
		super.onFirstTick(aBaseMetaTileEntity);
		if (aBaseMetaTileEntity.isClientSide())
			StructureLibAPI.queryAlignment((IAlignmentProvider) aBaseMetaTileEntity);
	}
}
