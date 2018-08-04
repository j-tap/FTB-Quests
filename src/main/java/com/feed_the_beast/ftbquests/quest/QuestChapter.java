package com.feed_the_beast.ftbquests.quest;

import com.feed_the_beast.ftblib.lib.config.ConfigGroup;
import com.feed_the_beast.ftblib.lib.config.ConfigInt;
import com.feed_the_beast.ftblib.lib.config.ConfigItemStack;
import com.feed_the_beast.ftblib.lib.config.ConfigList;
import com.feed_the_beast.ftblib.lib.config.ConfigString;
import com.feed_the_beast.ftblib.lib.icon.Icon;
import com.feed_the_beast.ftblib.lib.icon.IconAnimation;
import com.feed_the_beast.ftblib.lib.icon.ItemIcon;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public final class QuestChapter extends ProgressingQuestObject
{
	public final QuestFile file;
	public int index;
	public final ConfigString title;
	public final ConfigList<ConfigString> description;
	public final ConfigItemStack icon;
	public final List<Quest> quests;
	public final ConfigList<ConfigInt> dependencies;

	public QuestChapter(QuestFile f, NBTTagCompound nbt)
	{
		super(f.getID(nbt));
		file = f;
		title = new ConfigString(nbt.getString("title"));
		description = new ConfigList<>(new ConfigString(""));
		icon = new ConfigItemStack(new ItemStack(nbt.getCompoundTag("icon")), true);
		quests = new ArrayList<>();

		NBTTagList desc = nbt.getTagList("description", Constants.NBT.TAG_STRING);

		for (int i = 0; i < desc.tagCount(); i++)
		{
			description.add(new ConfigString(desc.getStringTagAt(i)));
		}

		dependencies = new ConfigList<>(new ConfigInt(1, 1, QuestFile.MAX_ID));

		for (int d : nbt.getIntArray("depends_on"))
		{
			dependencies.add(new ConfigInt(d));
		}

		NBTTagList questsList = nbt.getTagList("quests", Constants.NBT.TAG_COMPOUND);

		for (int j = 0; j < questsList.tagCount(); j++)
		{
			Quest quest = new Quest(this, questsList.getCompoundTagAt(j));
			quests.add(quest);
			file.map.put(quest.id, quest);
		}
	}

	@Override
	public QuestFile getQuestFile()
	{
		return file;
	}

	@Override
	public QuestObjectType getObjectType()
	{
		return QuestObjectType.CHAPTER;
	}

	@Override
	public void writeData(NBTTagCompound nbt)
	{
		nbt.setShort("id", id);
		nbt.setString("title", title.getString());

		if (!description.list.isEmpty())
		{
			NBTTagList list = new NBTTagList();

			for (ConfigString v : description)
			{
				list.appendTag(new NBTTagString(v.getString()));
			}

			nbt.setTag("description", list);
		}

		if (!icon.getStack().isEmpty())
		{
			nbt.setTag("icon", icon.getStack().serializeNBT());
		}

		if (!dependencies.isEmpty())
		{
			int[] ai = new int[dependencies.list.size()];

			for (int i = 0; i < dependencies.list.size(); i++)
			{
				ai[i] = dependencies.list.get(i).getInt();
			}

			nbt.setIntArray("dependencies", ai);
		}

		if (!quests.isEmpty())
		{
			NBTTagList questsList = new NBTTagList();

			for (Quest quest : quests)
			{
				NBTTagCompound questNBT = new NBTTagCompound();
				quest.writeData(questNBT);
				questsList.appendTag(questNBT);
			}

			nbt.setTag("quests", questsList);
		}
	}

	@Override
	public int getProgress(IProgressData data)
	{
		int progress = 0;

		for (Quest quest : quests)
		{
			progress += quest.getProgress(data);
		}

		return progress;
	}

	@Override
	public int getMaxProgress()
	{
		int maxProgress = 0;

		for (Quest quest : quests)
		{
			maxProgress += quest.getMaxProgress();
		}

		return maxProgress;
	}

	@Override
	public double getRelativeProgress(IProgressData data)
	{
		double progress = 0D;

		for (Quest quest : quests)
		{
			progress += quest.getRelativeProgress(data);
		}

		return progress / (double) quests.size();
	}

	@Override
	public void resetProgress(IProgressData data)
	{
		for (Quest quest : quests)
		{
			quest.resetProgress(data);
		}
	}

	@Override
	public Icon getIcon()
	{
		Icon i = ItemIcon.getItemIcon(icon.getStack());

		if (i.isEmpty())
		{
			List<Icon> list = new ArrayList<>();

			for (Quest quest : quests)
			{
				list.add(quest.getIcon());
			}

			i = IconAnimation.fromList(list, false);
		}

		return i;
	}

	@Override
	public ITextComponent getDisplayName()
	{
		return new TextComponentString(title.getString());
	}

	@Override
	public void deleteSelf()
	{
		super.deleteSelf();
		file.chapters.remove(this);
	}

	@Override
	public void deleteChildren()
	{
		for (Quest quest : quests)
		{
			quest.deleteChildren();
		}

		quests.clear();
	}

	@Override
	public void getConfig(ConfigGroup group)
	{
		group.add("title", title, new ConfigString(""));
		group.add("icon", icon, new ConfigItemStack(ItemStack.EMPTY, true));
		group.add("description", description, new ConfigList<>(new ConfigString("")));
		group.add("dependencies", dependencies, new ConfigList<>(new ConfigInt(1, 1, QuestFile.MAX_ID)));
	}
}