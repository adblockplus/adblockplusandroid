/*
 * This file is part of Adblock Plus <https://adblockplus.org/>,
 * Copyright (C) 2006-2015 Eyeo GmbH
 *
 * Adblock Plus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * Adblock Plus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Adblock Plus.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.adblockplus.android.compat;

/**
 * Checked exception to indicate any problems/errors inside the compatibility layer (mostly reflection specific errors).
 */
public class CompatibilityException extends Exception
{
  private static final long serialVersionUID = -6583503345769050560L;

  public CompatibilityException(final String message)
  {
    super(message);
  }

  public CompatibilityException(final String message, final Throwable throwable)
  {
    super(message, throwable);
  }

  public CompatibilityException(final Throwable throwable)
  {
    super(throwable);
  }
}
