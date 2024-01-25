/*
 * All Rights Reserved (c) MoriyaShiine
 */

package moriyashiine.enchancement.common.reloadlisteners;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import moriyashiine.enchancement.common.Enchancement;
import moriyashiine.enchancement.common.util.BeheadingEntry;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.io.InputStream;
import java.io.InputStreamReader;

public class BeheadingReloadListener implements SimpleSynchronousResourceReloadListener {
	private static final Identifier ID = Enchancement.id("beheading");

	@Override
	public Identifier getFabricId() {
		return ID;
	}

	@Override
	public void reload(ResourceManager manager) {
		BeheadingEntry.DROP_MAP.clear();
		manager.findAllResources("beheading", path -> path.getNamespace().equals(Enchancement.MOD_ID) && path.getPath().endsWith(".json")).forEach((identifier, resources) -> {
			for (Resource resource : resources) {
				try (InputStream stream = resource.getInputStream()) {
					JsonObject object = JsonParser.parseReader(new JsonReader(new InputStreamReader(stream))).getAsJsonObject();
					identifier = new Identifier(identifier.getPath().substring(identifier.getPath().indexOf("/") + 1, identifier.getPath().length() - 5).replace("/", ":"));
					EntityType<?> entity_type = Registries.ENTITY_TYPE.get(identifier);
					Item drop = Registries.ITEM.get(new Identifier(JsonHelper.getString(object, "drop")));
					float chance = JsonHelper.getFloat(object, "chance");
					BeheadingEntry.DROP_MAP.put(entity_type, new BeheadingEntry(drop, chance));
				}
				catch (Exception ignored) {}
			}
		});
	}
}
