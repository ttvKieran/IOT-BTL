import React from 'react';
import { Link } from 'react-router-dom';

const Navbar = () => {
  return (
    <nav className="bg-white/80 backdrop-blur-md shadow-lg sticky top-0 z-50 border-b-2 border-gray-200">
      <div className="max-w-7xl mx-auto px-6">
        <div className="flex justify-between items-center h-20">
          {/* Logo */}
          <Link to="/" className="flex items-center space-x-3 hover:scale-105 transition-all duration-300">
            <div className="text-3xl">🌱</div>
            <span className="text-2xl font-bold bg-gradient-to-r from-green-600 to-emerald-500 bg-clip-text text-transparent">
              Smart Garden
            </span>
          </Link>

          {/* Navigation Links */}
          <div className="flex space-x-2">
            <NavLink to="/">Trang chủ</NavLink>
            <NavLink to="/control">Điều khiển</NavLink>
            <NavLink to="/dashboard">Thống kê</NavLink>
            <NavLink to="/chat">AI Chat</NavLink>
          </div>
        </div>
      </div>
    </nav>
  );
};

const NavLink = ({ to, children }) => {
  return (
    <Link
      to={to}
      className="px-6 py-2 rounded-lg font-medium text-gray-700 hover:bg-gradient-to-r hover:from-green-600 hover:to-emerald-500 hover:text-white transition-all duration-300 hover:-translate-y-0.5"
    >
      {children}
    </Link>
  );
};

export default Navbar;
