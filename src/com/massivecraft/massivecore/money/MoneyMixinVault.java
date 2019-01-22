package com.massivecraft.massivecore.money;

import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.MUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Collection;

public class MoneyMixinVault extends MoneyMixinAbstract
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final MoneyMixinVault i = new MoneyMixinVault();
	public static MoneyMixinVault get() { return i; }
	
	// -------------------------------------------- //
	// ACTIVATE & DEACTIVATE
	// -------------------------------------------- //
	
	public void activate()
	{
		if (Money.mixin() != null) return;
		Money.mixin(this);
	}
	
	public void deactivate()
	{
		Money.mixin(null);
	}
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public Economy getEconomy()
	{
		RegisteredServiceProvider<Economy> registeredServiceProvider = Bukkit.getServicesManager().getRegistration(Economy.class);
		if (registeredServiceProvider == null) return null;
		return registeredServiceProvider.getProvider();
	}
	
	// -------------------------------------------- //
	// ENABLED AND DISABLED
	// -------------------------------------------- //
	
	@Override
	public boolean enabled()
	{
		Economy economy = this.getEconomy();
		if (economy == null) return false;
		return economy.isEnabled();
	}
	
	// -------------------------------------------- //
	// FORMAT AND NAME
	// -------------------------------------------- //
	
	@Override
	public String format(double amount, boolean includeUnit)
	{
		if (includeUnit)
		{
			return this.getEconomy().format(amount);
		}
		else
		{
			int fractionalDigits = this.fractionalDigits();
			amount = prepare(amount);
			if (fractionalDigits < 0)
			{
				return String.valueOf(amount);
			}
			else if (fractionalDigits == 0)
			{
				return String.valueOf((int)amount);
			}
			else
			{
				return String.format("%." + fractionalDigits + "f", amount);
			}
		}
	}
	
	@Override
	public String singular()
	{
		return this.getEconomy().currencyNameSingular();		
	}
	
	@Override
	public String plural()
	{
		return this.getEconomy().currencyNamePlural();
	}
	
	// -------------------------------------------- //
	// FRACTIONAL DIGITS
	// -------------------------------------------- //
	
	@Override
	public int fractionalDigits()
	{
		return this.getEconomy().fractionalDigits();
	}
	
	// -------------------------------------------- //
	// EXISTS AND CREATE
	// -------------------------------------------- //
	
	@Override
	public boolean exists(String accountId)
	{
		Object econObject = getEconomyObject(accountId);
		OfflinePlayer op = econObject instanceof OfflinePlayer ? (OfflinePlayer) econObject : null;
		String name = econObject instanceof String ? (String) econObject : null;

		if (op != null) return this.getEconomy().hasAccount(op);
		if (name != null) return this.getEconomy().hasAccount(name);
		throw new RuntimeException();
	}
	
	@Override
	public boolean create(String accountId)
	{
		return this.ensureExists(accountId);
	}
	
	// -------------------------------------------- //
	// CHECK
	// -------------------------------------------- //
	
	@Override
	public double get(String accountId)
	{
		this.ensureExists(accountId);

		Object econObject = getEconomyObject(accountId);
		OfflinePlayer op = econObject instanceof OfflinePlayer ? (OfflinePlayer) econObject : null;
		String name = econObject instanceof String ? (String) econObject : null;

		if (op != null) return this.getEconomy().getBalance(op);
		if (name != null) return this.getEconomy().getBalance(name);
		throw new RuntimeException();
	}
	
	@Override
	public boolean has(String accountId, double amount)
	{
		this.ensureExists(accountId);

		Object econObject = getEconomyObject(accountId);
		OfflinePlayer op = econObject instanceof OfflinePlayer ? (OfflinePlayer) econObject : null;
		String name = econObject instanceof String ? (String) econObject : null;

		if (op != null) return this.getEconomy().has(op, amount);
		if (name != null) return this.getEconomy().has(name, amount);
		throw new RuntimeException();
	}
	
	// -------------------------------------------- //
	// MODIFY
	// -------------------------------------------- //
	
	@Override
	public boolean move(String fromId, String toId, String byId, double amount, Collection<String> categories, Object message)
	{
		Economy economy = this.getEconomy();

		// Ensure positive direction
		if (amount < 0)
		{
			amount *= -1;
			String temp = fromId;
			fromId = toId;
			toId = temp;
		}

		OfflinePlayer offlinePlayerFrom = IdUtil.getOfflinePlayer(fromId);
		OfflinePlayer offlinePlayerTo = IdUtil.getOfflinePlayer(toId);
		
		// Ensure the accounts exist
		if (fromId != null) this.ensureExists(fromId);
		if (toId != null) this.ensureExists(toId);
		
		// Subtract From
		if (fromId != null)
		{
			if (!economy.withdrawPlayer(offlinePlayerFrom, amount).transactionSuccess())
			{
				return false;
			}
		}
		
		// Add To
		if (toId != null)
		{
			if (!economy.depositPlayer(offlinePlayerTo, amount).transactionSuccess())
			{
				if (fromId != null)
				{
					// Undo the withdraw
					economy.depositPlayer(offlinePlayerFrom, amount);
				}
				return false;
			}
		}
		
		return true;
	}
	
	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //
	
	public boolean ensureExists(String accountId)
	{
		Economy economy = this.getEconomy();

		Object econObject = getEconomyObject(accountId);
		OfflinePlayer op = econObject instanceof OfflinePlayer ? (OfflinePlayer) econObject : null;
		String name = econObject instanceof String ? (String) econObject : null;
		
		if (op != null && economy.hasAccount(op)) return true;
		if (name != null && economy.hasAccount(name)) return true;
		
		if (op != null && !economy.createPlayerAccount(op)) return false;
		if (name != null && !economy.createPlayerAccount(name)) return false;
		
		if (MUtil.isUuid(accountId)) return true;

		// What is this for???
		//double balance = economy.getBalance(getOfflinePlayer(accountId));
		//economy.withdrawPlayer(getOfflinePlayer(accountId), balance);
		
		return false;
	}

	public static Object getEconomyObject(String accountId)
	{
		// Because of Factions we have this crazy-workaround.
		// Where offlineplayers will be used when possible
		// but otherwise names will.
		OfflinePlayer ret = IdUtil.getOfflinePlayer(accountId);
		if (ret != null) return ret;
		else return accountId;
	}

}
